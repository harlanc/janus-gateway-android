package computician.janusclientapi;


import android.content.Context;
import android.opengl.EGLContext;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.math.BigInteger;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import android.media.AudioManager;



public class JanusAudioBridge {
    public static final String REQUEST = "request";
    public static final String MESSAGE = "message";
    public static final String PARTICIPANTS = "participants";
    private final String JANUS_URI = "wss://v2.gvrcraft.com:8989";
    private JanusPluginHandle handle = null;
    private AudioManager aumanager = null;

    private JanusServer janusServer;
    private BigInteger myid;
    private String user_name = GetGUID();
    final private int roomid = 1234;
    private Context context;

    public void SetContext(Context con)
    {
        context = con;
    }

    private static String GetGUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void init() {
        try {

            initializeMediaContext(context, true, false, false, null);
            Start();
            aumanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        } catch (Exception ex) {
            Log.e("computician.janusclient", ex.getMessage());
        }
    }

    public JanusAudioBridge() {
        janusServer = new JanusServer(new JanusGlobalCallbacks());
    }

    public void openMic()
    {
        setMute(false);
        aumanager.setMicrophoneMute(false);
    }
    public void closeMic()
    {
        aumanager.setMicrophoneMute(true);
        setMute(true);
    }

    public void openSpeaker()
    {
        aumanager.setSpeakerphoneOn(true);
    }
    public void closeSpeaker()
    {
        aumanager.setSpeakerphoneOn(false);
    }


    private void setMute(boolean value)
    {
        if(handle != null){
            try{
                JSONObject msg = new JSONObject();
                JSONObject body = new JSONObject();
                body.put(REQUEST, "configure");
                body.put("muted", value);
                msg.put(MESSAGE, body);

                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));

            }catch (Exception ex){

            }
        }
    }




    public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {

        private void publishOwnFeed() {
            if(handle != null) {
                handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                    @Override
                    public void onSuccess(JSONObject obj) {
                        try
                        {
                            JSONObject msg = new JSONObject();
                            JSONObject body = new JSONObject();
                            body.put(REQUEST, "configure");
                            body.put("muted", true);
                            msg.put(MESSAGE, body);
                            msg.put("jsep", obj);
                            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                        }catch (Exception ex) {

                        }
                    }

                    @Override
                    public JSONObject getJsep() {
                        return null;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        JanusMediaConstraints cons = new JanusMediaConstraints();
                        cons.setVideo(null);
                        return cons;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return true;
                    }

                    @Override
                    public void onCallbackError(String error) {

                    }
                });
            }
        }

        private void registerUsername() {
            if(handle != null) {
                JSONObject obj = new JSONObject();
                JSONObject msg = new JSONObject();
                try
                {
                    obj.put(REQUEST, "join");
                    obj.put("room", roomid);
//                    obj.put("ptype", "publisher");
                    obj.put("display", user_name);
                    msg.put(MESSAGE, obj);
                }
                catch(Exception ex)
                {

                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }

        @Override
        public void success(JanusPluginHandle pluginHandle) {
            handle = pluginHandle;
            registerUsername();
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsepLocal) {
            try
            {
                String event = msg.getString("audiobridge");
                if(event.equals("joined")) {
                    myid = new BigInteger(msg.getString("id"));
                    publishOwnFeed();
                    if(msg.has(PARTICIPANTS)){
                        JSONArray pubs = msg.getJSONArray(PARTICIPANTS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
//                            BigInteger tehId = new BigInteger(pub.getString("id"));
//                            newRemoteFeed(tehId);
                            String id = pub.getString("id");
                            String display = pub.getString("display");
                            String setup = pub.getString("setup");
                            String muted = pub.getString("muted");

                            //TODO display info on GUI
                        }
                    }
                } else if(event.equals("destroyed")) {

                } else if(event.equals("event")) {
                    if(msg.has(PARTICIPANTS)){
                        JSONArray pubs = msg.getJSONArray(PARTICIPANTS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
//                            newRemoteFeed(new BigInteger(pub.getString("id")));
                            String id = pub.getString("id");
                            String display = pub.getString("display");
                            String setup = pub.getString("setup");
                            String muted = pub.getString("muted");

                            //TODO display info on GUI

                        }
                    } else if(msg.has("leaving")) {

                    } else if(msg.has("unpublished")) {

                    } else {
                        //todo error
                    }
                }
                if(jsepLocal != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
                }
            }
            catch (Exception ex)
            {

            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {
            // stream.audioTracks.get(0).addRenderer(new VideoRenderer(localRender));
        }

        @Override
        public void onRemoteStream(MediaStream stream) {

        }

        @Override
        public void onDataOpen(Object data) {

        }

        @Override
        public void onData(Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_AUDIO_BRIDGE;
        }

        @Override
        public void onCallbackError(String error) {

        }

        @Override
        public void onDetached() {

        }
    }

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {
        public void onSuccess() {
            janusServer.Attach(new JanusPublisherPluginCallbacks());
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public String getServerUri() {
            return JANUS_URI;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            return new ArrayList<PeerConnection.IceServer>();
        }

        @Override
        public Boolean getIpv6Support() {
            return Boolean.FALSE;
        }

        @Override
        public Integer getMaxPollEvents() {
            return 0;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public boolean initializeMediaContext(Context context, boolean audio, boolean video, boolean videoHwAcceleration, EGLContext eglContext){
        return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration, eglContext);
    }

    public void Start() {
        janusServer.Connect();
    }
}
