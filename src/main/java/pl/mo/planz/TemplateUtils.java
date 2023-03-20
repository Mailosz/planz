package pl.mo.planz;

import java.util.List;
import java.util.Map;

public class TemplateUtils {
    
    public static String replaceFields(String template, Map<String, String> map, boolean showEmpty) {

        StringBuilder sb = new StringBuilder(template.length());
        int a = 0;
        int b = 0;
        do  {
            b = template.indexOf("@", a);


            if (b == -1) {
                sb.append(template.substring(a));
                break;
            } else {
                sb.append(template.substring(a, b));

                a = template.indexOf("@", b+1);

                if (a == -1) {
                    sb.append(template.substring(b+1));
                    break;
                } else if (b == a + 1) {
                    sb.append("@"); // @@ zamieniamy na @
                } else {
                    String valueName = template.substring(b+1, a);

                    String value = map.get(valueName);
                    if (value == null) {
                        if (showEmpty){
                            sb.append("@");
                            sb.append(valueName);
                            sb.append("@");
                        }
                    } else {
                        sb.append(value);
                    }
                }
                a++;
            }

        } while (a < template.length());
        return sb.toString();
    }

}
