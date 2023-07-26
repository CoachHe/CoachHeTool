package file;

import java.io.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: coachhe
 * Date: 2023/2/1
 * Time: 15:23
 * Description:
 */
public class FileUtil {

    /**
     * @param filePath 文件的全路径
     * @param list 将文件的每一行读入list中
     */
    public static void fileToList(String filePath, List<String> list) {
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader br = new BufferedReader(fileReader);
            String line = null;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除文件夹
     * @param file
     */
    public static void deleteDir(File file) {
        if (file != null) {
            // 文件不存在则直接返回
            if (!file.exists()) {
                return;
            }
            // 如果是文件则直接删除
            if (file.isFile()) {
                boolean deleteResult = file.delete();
                int tryCount = 0;
                while (!deleteResult && tryCount++ <= 10) {
                    System.gc();
                    deleteResult = file.delete();
                }
            }
            // 如果是文件夹则遍历删除
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    deleteDir(files[i]);
                }
            }
            // 最后删除
            boolean deleteResult = file.delete();
            int tryCount = 0;
            while (!deleteResult && tryCount++ <= 10) {
                System.gc();
                deleteResult = file.delete();
            }
        }
    }

}
