import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Taylor on 7/16/2016.
 */
class ButtonPressSender {

    private final String URL;
    private final String MAC;
    private final long time;
    private int status;

    public ButtonPressSender(String URL, String MAC, long time) {
        this.URL = URL;
        this.MAC = MAC;
        this.time = time;
        this.status = 0;
    }

    public void send() throws IOException {
        List<NameValuePair> payload = new ArrayList<>();
        payload.add(new BasicNameValuePair("m", MAC + ""));
        payload.add(new BasicNameValuePair("t", time + ""));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(payload, Consts.UTF_8);
        entity.setContentType("application/x-www-form-urlencoded");
        HttpPost httpPost = new HttpPost(URL);
        httpPost.setEntity(entity);

        HttpClient httpClient = HttpClients.createDefault();
        HttpResponse httpResponse = httpClient.execute(httpPost);

        status = httpResponse.getStatusLine().getStatusCode();
    }

    public int getStatusCode() {
        return this.status;
    }
}
