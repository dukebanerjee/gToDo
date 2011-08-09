package test.apps.gtodo.service;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;

public class HttpClientUtils {
    public static String readEntityAsString(HttpEntity entity) throws IOException {
        InputStreamReader r = new InputStreamReader(entity.getContent());
        int length;
        StringBuffer b = new StringBuffer(512);
        char[] buffer = new char[512];
        while ((length = r.read(buffer)) != -1) {
            b.append(buffer, 0, length);
        }
        return b.toString();
    }
}
