# Multi Party

[日本語](./README.md) | [English](./README.en.md)

This is a library for easy implementation of group video chat with SkyWay(http://nttcom.github.io/skyway/) for Android.

### Setting

1. Add "SkyWay.aar"(Download from [here](https://github.com/nttcom/SkyWay-Android-SDK)) to the "SkyWay/".

## API reference

### MultiParty

####Property
* opened (boolean)
    * Is true if the connection is open.

####Constructor
```java
Context context = getApplicationContext();

MultiPatryOption options = new MultiPatryOption();
options.key = {API-KEY};
options.domain = {DOMAIN};

MultiParty multiparty = new MultiParty(context,options);
```
* context (AndroidContext)
    * Specify ApplicationContext Object.
* options (MultiPartyOption)
    * Specify connection settings.

#### MultiPartyOption

* key (String)
    * an API key obtained from [SkyWay Web Site](https://skyway.io/ds/)
* domain (String)
    * The domain registered with the API key on ([the SkyWay developer's dashboard](https://skyway.io/ds/))。**Required**。
    * room (String)
        * room name。Unique Room ID is made from 'room','locationHost' and 'locationPath'. **dafault ''**
    * locationHost (String)
        * hostname of webapp(when you connect with [JS SDK](https://github.com/nttcom/SkyWay-MultiParty)). Unique Room ID is made from 'room','locationHost' and 'locationPath'.
    * locationPath (String)
        * path of webapp(when you connect with [JS SDK](https://github.com/nttcom/SkyWay-MultiParty)). Unique Room ID is made from 'room','locationHost' and 'locationPath'.
*  identity (String)
    * user id
* reliable (boolean)
    * **true** indicates reliable data transfer (data channel). ```default : false```
* selialization (SerializationEnum)
    * set data selialization mode. ```default : BINARY```

    ```
    BINARY
    BINARY_UTF8
    JSON
    NONE
    ```

* constraints(MediaConstraints)
    * MediaConstraints[Android SDK API Reference](https://nttcom.github.io/skyway/docs/#Android-mediaconstraints)
* polling (boolean)
    * **true** indicates check user list via server polling. ```default: true```
* polling_interval (int)
    * polling interval in msec order. ```default: 3000```

* debug (DebugLevelEnum)
    * debug log level appeared in console.
```
NO_LOGS
ONLY_ERROR
ERROR_AND_WARNING
ALL_LOGS
```

* host (string)
    * peer server host name. ```default: skyway.io```
* port (number)
    * peer server port number. ```default: 443```
* path (String)
    * peer server path. ```default: /```
* secure (boolean)
    * true means peer server provide tls.
* config (object)
   * it indicates custom ICE server configuration [IceConfig](https://nttcom.github.io/skyway/docs/#Android-iceconfig).
 * useSkyWayTurn (boolean)
     * true if you're using SkyWay's TURN server. Defaults to false. You must apply [here](https://skyway.io/ds/turnrequest) to use this feature.

### start

Connect to the SkyWay server and all peers.
```java
multiparty.start();
```

### on(event,callback)

Set event callback for MultiParty.

* event (MultiPartyEventEnum)
    * event type
* callback (OnCallback)
    * Specifies the callback function to call when the event is triggered.


#### 'open'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.OPEN, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    try
    {
        peerId = object.getString(“peer-id”);
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when a connection to SkyWay server has established.
* **peerId** : id of current window.

#### 'MultiPartyEventEnum.MY_MS'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.MY_MS, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    MediaStream stream = null;
    try
    {
        peerId = object.getString(“id”);
        stream = (MediaStream)object.get("src");
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when this window's video/audio stream has setuped.
* **src** : src for captured stream.
* **id** : current window's id.

#### 'MultiParty.MultiPartyEventEnum.PEER_MS'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.PEER_MS, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    MediaStream stream = null;
    boolean reconnect = false;
    try
    {
        peerId = object.getString(“id”);
        stream = (MediaStream)object.get("src");
        reconnect = object.getBoolean("reconnect");
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when peer's av stream has setuped.
* **src** : Object of peer's stream.
* **id** : peer's id.
* **reconnect** :  **true** when connected via reconnect method.

#### 'MultiParty.MultiPartyEventEnum.PEER_SS'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.PEER_SS, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    MediaStream stream = null;
    boolean reconnect = false;
    try
    {
        peerId = object.getString(“id”);
        stream = (MediaStream)object.get("src");
        reconnect = object.getBoolean("reconnect");
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when peer's screen captrure stream has setuped.
* **src** : Object of peer's screen capture stream.
* **id** :  peer's id.
* **reconnect** : **true** when connected via reconnect method.

#### 'MultiParty.MultiPartyEventEnum.MS_CLOSE'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.MS_CLOSE, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    try
    {
        peerId = object.getString(“id”);
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when peer's media stream has closed.
* **peer-id** : peer's id.

#### 'MultiParty.MultiPartyEventEnum.SS_CLOSE'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.SS_CLOSE, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    try
    {
        peerId = object.getString(“id”);
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when peer's screen cast stream has closed.
* **peer-id** : peer's id.

#### 'dc_open'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.dc_open, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    try
    {
        peerId = object.getString(“id”);
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when the connection for data channel with peer is setuped.
* **peer-id** : peer's id.

#### 'message'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.MESSAGE new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    Object data = null;
    try
    {
        peerId = object.getString(“id”);
        data  = object.get("data");
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when receive message from peer.
* **peer-id** : peer's id.
* **data** : Received data.

#### 'MultiParty.MultiPartyEventEnum.DC_CLOSE'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.DC_CLOSE, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    String peerId = null;
    try
    {
        peerId = object.getString(“id”);
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```
* Emitted when data connection has closed with peer.
* **peer-id** : peer's id.

#### 'MultiParty.MultiPartyEventEnum.ERROR'
```java
multiparty.on(MultiParty.MultiPartyEventEnum.ERROR, new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    PeerError peerError = null;
    try
    {
        peerError = object.getString(“peerError”);
    }catch (JSONException e) {
      e.printStackTrace();
    }
  }
});
```

* Emitted when an error occurs.
* **error** : Error object.[PeerError](https://nttcom.github.io/skyway/docs/#Android-peererror)

### mute

Mute current video/audio.

void mute(boolean video,boolean sudio);

* video (boolean)
    * true:mute video false:unmute video
* audio (boolean)
    * true:mute audio false:unmute audio

```java
multiparty.mute(true,true);
```

### removePeer

Close peer's media stream and data stream.

boolean removePeer(String peerId);

* peerId (String)
    * peerId to be deleted

```java
multiparty.removePeer({peer-id});
```

### send

boolean send(Object data)

send data

* data (Object)

```java
String message ＝ ”Hello”；
multiparty.send(MESSAGE);
```

### close

Close every connection.

boolean close();

```java
multiparty.close();
```

### listAllPeers(OnCallback() callback)
Get all of the connected peer ids.

boolean listAllPeers(OnCallback() callback)

```java
party.listAllPeers(new OnCallback() {
  @Override
  public void onCallback(JSONObject object) {
    JSONArray list = null;
    try
    {
      list = object.getJSONArray(“peers”);
    }
    catch (JSONException e) {
    }
});
```


## LICENSE & Copyright

[LICENSE](./LICENSE)
