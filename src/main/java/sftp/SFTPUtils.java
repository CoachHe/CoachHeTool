package sftp;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Vector;

import static com.tencent.sftp.Constants.MAX_RETRIES;

/**
 * Created with IntelliJ IDEA.
 * User: coachhe
 * Date: 2023/2/6
 * Time: 16:36
 * Description:
 */
@Slf4j
public class SFTPUtils {

//    private static final Logger logger = LoggerFactory.getLogger(SFTPUtils.class);

    private String remote_host = "";
    private String username = "";
    private String private_key_path = "";
    private int remote_port = 22;

    public SFTPUtils(String remote_host, int remote_port, String username, String private_key_path) throws JSchException {
        this.remote_host = remote_host;
        this.username = username;
        this.private_key_path = private_key_path;
        this.remote_port = remote_port;
    }

    /**
     * 连接sftp，返回ChannelSftp
     */
    public ChannelSftp connect() throws JSchException {
        JSch jSch = new JSch();
        Session jschSession = jSch.getSession(this.username, this.remote_host, this.remote_port);
        jSch.addIdentity(this.private_key_path);
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
//        sshConfig.put("kex","diffie-hellman-group1-sha1");
        jschSession.setConfig(sshConfig);
        log.info(String.format("start connecting to %s, user name is %s", this.remote_host, this.username));
        jschSession.connect(20000);
        Channel channel = jschSession.openChannel("sftp");
        channel.connect(20000);
        ChannelSftp sftp = (ChannelSftp) channel;
        log.info(String.format("%s connect to %s successfully, current time stamp is %s", this.username, this.remote_host, System.currentTimeMillis() / 1000));

        return sftp;
    }

    /**
     * 上传文件
     *
     * @param directory  上传的目录
     * @param uploadFile 要上传的文件
     */
    public boolean upload(String directory, String uploadFile) throws SftpException, IOException {
        try {
            ChannelSftp sftp = connect();

            sftp.cd(directory);
            File file = new File(uploadFile);
            FileInputStream fileInputStream = new FileInputStream(file);
            sftp.put(fileInputStream, file.getName());

            fileInputStream.close();
            sftp.exit();
            sftp.disconnect();
            sftp.getSession().disconnect();

            return true;
        } catch (JSchException je) {
            je.printStackTrace();
            log.error(String.format("connect to %s failed, error: %s, retry", username, je));
            upload(directory, uploadFile);
        }

        return false;
    }

    /**
     * 下载文件
     *
     * @param directory    下载目录
     * @param downloadFile 下载的文件
     * @param saveFile     存在本地的路径
     */
    public File download(String directory, String downloadFile, String saveFile) {

        log.info(String.format("start downloading files, save path is %s, current time stamp is %s", saveFile, System.currentTimeMillis() / 1000));

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                ChannelSftp sftp = connect();
                sftp.cd(directory);
                File file = new File(saveFile);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                sftp.get(downloadFile, fileOutputStream);
                fileOutputStream.close();
                sftp.exit();
                sftp.disconnect();
                sftp.getSession().disconnect();

                log.info(String.format("download files from %s successfully, save path is %s, current time stamp is %s", this.username, saveFile, System.currentTimeMillis() / 1000));

                return file;
            } catch (JSchException | SftpException | IOException e) {
                if (i == MAX_RETRIES - 1) {
                    break;
                }
                log.error(String.format("download file %s failed, errortag: %s, error: %s, retry", downloadFile, e, e.getMessage()));
            }
        }

        return null;
    }

    /**
     * 下载文件
     *
     * @param downloadFilePath 下载的文件完整目录
     * @param saveFile         存在本地的路径
     */
    public File download(String downloadFilePath, String saveFile) {

        log.info(String.format("start downloading files, save path is %s, current time stamp is %s", saveFile, System.currentTimeMillis() / 1000));

        // 设置重试
        for (int j = 0; j < MAX_RETRIES; j++) {
            try {
                ChannelSftp sftp = connect();
                // 获取需要下载的文件
                int i = downloadFilePath.lastIndexOf('/');
                if (i == -1)
                    return null;
                sftp.cd(downloadFilePath.substring(0, i));
                File file = new File(saveFile);
                // 文件夹不存在则先创建
                String savePath = saveFile.substring(0, saveFile.lastIndexOf("/"));
                if (!Files.exists(Paths.get(savePath))) {
                    Files.createDirectories(Paths.get(savePath));
                }
                // 若文件存在则将其删除
                Files.deleteIfExists(Paths.get(saveFile));
                // 获取sftp的输出流参数
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                sftp.get(downloadFilePath.substring(i + 1), fileOutputStream);
                fileOutputStream.close();
                sftp.exit();
                sftp.disconnect();
                sftp.getSession().disconnect();

                log.info(String.format("download files from %s successfully, save path is %s, current time stamp is %s", this.username, saveFile, System.currentTimeMillis() / 1000));

                return file;
            } catch (SftpException | JSchException | IOException e) {
                if (j == MAX_RETRIES - 1) {
                    break;
                }
                log.error(String.format("download file %s failed, errortag: %s, error: %s, retry", downloadFilePath, e, e.getMessage()));
            }
        }

        return null;
    }

    /**
     * 删除文件
     *
     * @param directory  要删除文件所在目录
     * @param deleteFile 要删除的文件
     */
    public void delete(String directory, String deleteFile) throws SftpException {
        try {
            ChannelSftp sftp = connect();
            sftp.cd(directory);
            sftp.rm(deleteFile);

            sftp.exit();
            sftp.disconnect();
            sftp.getSession().disconnect();
        } catch (JSchException je) {
            je.printStackTrace();
            log.info(String.format("connect to %s failed, error: %s, retry", username, je));
        }
    }

    /**
     * 列出目录下的文件
     */
    public Vector listFiles(String pathName) {

        log.info(String.format("start listing files from %s, current time stamp is %s", this.username, System.currentTimeMillis() / 1000));

        try {

            ChannelSftp sftp = connect();

            Vector lsData = sftp.ls(pathName);
            sftp.exit();
            sftp.disconnect();
            sftp.getSession().disconnect();

            log.info(String.format("list files from %s OK, current time stamp is %s", this.username, System.currentTimeMillis() / 1000));

            return lsData;
        } catch (JSchException je) {

            log.error(String.format("connect to %s failed, error: %s, retry", username, je));

            listFiles(pathName);
        } catch (SftpException se) {

            log.error(String.format("connect to %s failed, error: %s, retry", username, se));

            listFiles(pathName);
        }

        return null;
    }

}
