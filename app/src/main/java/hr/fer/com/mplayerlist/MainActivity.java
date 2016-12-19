package hr.fer.com.mplayerlist;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hr.fer.com.mplayerlist.customView.MomentsView;
import hr.fer.com.mplayerlist.model.MomentData;

public class MainActivity extends AppCompatActivity {
    private LinearLayout rootLyt;
    private MomentsView videoList;
    private HashMap<Integer, VideoData> metaData = new HashMap<>();
    private List<MomentData> videos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        App.registerActivity(this);
        rootLyt = (LinearLayout)findViewById(R.id.rootLyt);
        videoList = new MomentsView(this);
        videoList.setLayoutManagerOrientation(RecyclerView.VERTICAL);
        rootLyt.addView(videoList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(videos.isEmpty()) {
            buildMetaData();
            copyDataFromAssets("data");
            for (VideoData v : metaData.values()) {
                videos.add(new MomentData(v.id, v.starMs, v.endMs, v.title, v.thumbnailUri, v.videoUri));
            }
            videoList.bindData(videos);
            videoList.setIsViewInFocus(true);
        }else videoList.onActivityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoList.onActivityPaused(this);
    }

    private void buildMetaData() {
        metaData.put(374, new VideoData(374, 477, 4802, "Football"));
        metaData.put(475, new VideoData(475, 684, 3393, "Biber"));
        metaData.put(479, new VideoData(479, 411, 6367, "Trump"));
        metaData.put(450, new VideoData(450,1103, 3611, "Basketball"));
        metaData.put(168, new VideoData(168, 102, 6664, "Motorbike"));
    }

    private void copyDataFromAssets(String dataDir) {
        String copyToDirPath = getFilesDir().getAbsolutePath() + File.separator + "data";
        File copyDir = new File(copyToDirPath);
        if(copyDir.exists()){
            String[] cachedFiles = copyDir.list();
            if(cachedFiles != null && cachedFiles.length > 0) {
                for(String f: cachedFiles) {
                    int id = Integer.valueOf(f.substring(0, f.length()-4));
                    Uri fileUri = Uri.parse("file://" + copyToDirPath + File.separator + f);
                    if(f.endsWith(".mp4")) metaData.get(id).videoUri = fileUri;
                    else metaData.get(id).thumbnailUri = fileUri;
                }
                return;
            }
        }else copyDir.mkdirs();
        try {
            String[] fileNames = getAssets().list(dataDir);
            if(fileNames.length > 0) {
                for(String fname: fileNames) {
                    if(!fname.endsWith(".mp4") && !fname.endsWith(".jpg")) continue;

                    int id = Integer.valueOf(fname.substring(0, fname.length()-4));
                    Uri fileUri = Uri.parse("file://" + copyToDirPath + File.separator + fname);
                    if(fname.endsWith(".mp4")) metaData.get(id).videoUri = fileUri;
                    else metaData.get(id).thumbnailUri = fileUri;

                    InputStream is = getAssets().open(dataDir + File.separator + fname);
                    File outFile = new File(copyToDirPath, fname);
                    if(outFile.exists()) continue;
                    else outFile.createNewFile();

                    OutputStream os = new FileOutputStream(outFile);
                    copyFile(is, os);
                    is.close();
                    is = null;
                    os.flush();
                    os.close();
                    os = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static class VideoData{
        int id;
        long starMs;
        long endMs;
        String title;
        Uri videoUri;
        Uri thumbnailUri;

        public VideoData(int id, long starMs, long endMs, String title) {
            this.id = id;
            this.starMs = starMs;
            this.endMs = endMs;
            this.title = title;
        }
    }
}
