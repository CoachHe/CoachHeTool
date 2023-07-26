package cos;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Transfer;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferProgress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author coachhe
 * @date 2023/2/1 11:11
 * @description
 */
public class TransferManagerFactory {
    // 私有化构造器，让外部无法自己创建
    private TransferManagerFactory(){
    }
    private static TransferManager transferManager;
    // 创建 TransferManager 实例，这个实例用来后续调用高级接口
    public static TransferManager createTransferManager(String region ,String secretId, String secretKey) {
        // 若transferManager不为空，则直接返回
        if (transferManager != null) {
            return transferManager;
        } else { // 否则以加锁方式创建一个transferManager
            // 创建一个 COSClient 实例，这是访问 COS 服务的基础实例。
            // 详细代码参见本页: 简单操作 -> 创建 COSClient
            COSClient cosClient = createCOSClient(region, secretId, secretKey);
            // 自定义线程池大小，建议在客户端与 COS 网络充足（例如使用腾讯云的 CVM，同地域上传 COS）的情况下，设置成16或32即可，可较充分的利用网络资源
            // 对于使用公网传输且网络带宽质量不高的情况，建议减小该值，避免因网速过慢，造成请求超时。
            ExecutorService threadPool = Executors.newFixedThreadPool(32);
            // 传入一个 threadpool, 若不传入线程池，默认 TransferManager 中会生成一个单线程的线程池。
            TransferManager transferManager = new TransferManager(cosClient, threadPool);
            return transferManager;
        }
    }
    public static void shutdownTransferManager(TransferManager transferManager) {
        // 指定参数为 true, 则同时会关闭 transferManager 内部的 COSClient 实例。
        // 指定参数为 false, 则不会关闭 transferManager 内部的 COSClient 实例。
        transferManager.shutdownNow(true);
    }
    // 创建 COSClient 实例，这个实例用来后续调用请求
    public static COSClient createCOSClient(String region, String secretId, String secretKey) {
        // 设置用户身份信息。
        // SECRETID 和 SECRETKEY 请登录访问管理控制台 https://console.tencentcloud.com/cam/capi 进行查看和管理
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // ClientConfig 中包含了后续请求 COS 的客户端设置：
        ClientConfig clientConfig = new ClientConfig();
        // 设置 bucket 的地域
        // COS_REGION 请参照 https://www.tencentcloud.com/document/product/436/6224?from_cn_redirect=1
        clientConfig.setRegion(new Region(region));
        // 设置请求协议, http 或者 https
        // 5.6.53 及更低的版本，建议设置使用 https 协议
        // 5.6.54 及更高版本，默认使用了 https
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 以下的设置，是可选的：
        // 设置 socket 读取超时，默认 30s
        clientConfig.setSocketTimeout(30*1000);
        // 设置建立连接超时，默认 30s
        clientConfig.setConnectionTimeout(30*1000);
        // 如果需要的话，设置 http 代理，ip 以及 port
//        clientConfig.setHttpProxyIp("httpProxyIp");
        clientConfig.setHttpProxyPort(80);
        // 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }
    // 可以参考下面的例子，结合实际情况做调整
    public static void showTransferProgress(Transfer transfer) {
        System.out.println(transfer.getDescription());
        // transfer.isDone() 查询下载是否已经完成
        while (transfer.isDone() == false) {
            try {
                // 每 2 秒获取一次进度
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return;
            }
            TransferProgress progress = transfer.getProgress();
            long sofar = progress.getBytesTransferred();
            long total = progress.getTotalBytesToTransfer();
            double pct = progress.getPercentTransferred();
            System.out.printf("progress: [%d / %d] = %.02f%%\n", sofar, total, pct);
        }
        // 完成了 Completed，或者失败了 Failed
        System.out.println(transfer.getState());
    }
}