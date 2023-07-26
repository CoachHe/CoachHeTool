package date;


import org.apache.commons.lang.time.DateFormatUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: coachhe
 * Date: 2023/2/1
 * Time: 15:00
 * Description:
 */
public class DateUtil {

    /**
     *
     * @param dateStr 输入的时间，需要是yyyyMMdd格式，例如20230201
     * @param splitter  希望得到的年月日之间的分隔符格式，例如"_"，置为空则说明不需要分隔符
     * @return 返回当前时间以及相同格式的前一天的时间，例如[20230201, 20230131]或者[2023_02_01, 2023_01_31]
     */
    public static List<String> getDate(String dateStr, String splitter) throws ParseException {
        // 判断dateStr是否为空字符串，如果为空则默认取到前一天
        Date date = null;
        String format = "yyyy" + splitter + "MM" + splitter + "dd";
        if ("".equals(dateStr)) {
            date = new Date();
            date = new Date(date.getTime() - 24 * 60 * 60 * 1000);
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            // 将输入时间按照分隔符进行分割，例如20230101转化为2023-01-01
            dateStr = dateStr.substring(0, 4) + splitter + dateStr.substring(4, 6) + splitter + dateStr.substring(6);
            date = simpleDateFormat.parse(dateStr);
        }
        String dateFormatStr = DateFormatUtils.format(date, format);
        // 获取前一天
        Date yesterday = new Date(date.getTime() - 24 * 60 * 60 * 1000);
        String yesterdayStr = DateFormatUtils.format(yesterday, format);
        // 将日期加入列表中
        List<String> resList = new ArrayList<>();
        resList.add(dateFormatStr);
        resList.add(yesterdayStr);
        return resList;
    }

}
