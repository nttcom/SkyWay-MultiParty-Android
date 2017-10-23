日本語 | [English](./README.en.md)

# Deprecated!

このレポジトリは、2018年3月に提供を終了する旧SkyWayのAndroid SDK向けMultiPartyライブラリです。[新しいSkyWay](https://webrtc.ecl.ntt.com/?origin=skyway)への移行をお願いします。

すでに新しいSkyWayをご利用の方は、[MeshRoomクラス](https://webrtc.ecl.ntt.com/android-reference/classio_1_1skyway_1_1_peer_1_1_mesh_room.html)および[SFURoomクラス](https://webrtc.ecl.ntt.com/android-reference/classio_1_1skyway_1_1_peer_1_1_s_f_u_room.html)をご覧ください。

# Multi Party

[SkyWay](http://nttcom.github.io/skyway/)を用い、多人数参加のグループビデオチャットを簡単に開発できるAndroid向けのライブラリです。

### 利用準備

1. SkyWay-SDK-Android([skyway.aar](https://github.com/nttcom/SkyWay-Android-SDK)) を SkyWay 以下に配置

## APIリファレンス

### MultiParty

#### プロパティ
* opened (boolean)
    * MultiPartyの接続状態

#### コンストラクタ
```java
Context context = getApplicationContext();

MultiPatryOption options = new MultiPatryOption();
options.key = {API-KEY};
options.domain = {DOMAIN};

MultiParty multiparty = new MultiParty(context,options);
```
* context (AndroidContext)
    * ApplicationContextオブジェクトを指定
* options (MultiPartyOption)
    * 設定情報オブジェクトを指定

#### MultiPartyOption

 * key (String)
     * API key([skyway](https://skyway.io/ds/)から取得)。**必須**。
 * domain (String)
     * APIキーに紐付くドメイン([skyway](https://skyway.io/ds/)から登録)。**必須**。
 * room (String)
     * ルーム名。room,locationHost,locationPathからユニークなルームIDを作成する。指定がない場合は""
 * locationHost (String)
     * [JS版](https://github.com/nttcom/SkyWay-MultiParty)との接続時にWebアプリの設置ホストを指定。room,locationHost,locationPathからユニークなルームIDを作成する。
 * locationPath (String)
     * [JS版](https://github.com/nttcom/SkyWay-MultiParty)との接続時にWebアプリの設置パスを指定。room,locationHost,locationPathからユニークなルームIDを作成する。
 *  identity (String)
     * 自身のピアID指定。指定がない場合は""
 * reliable (boolean)
     * データチャンネルで信頼性のあるデータ転送を行う。デフォルト値は **false**。
 * selialization (SerializationEnum)
     * データチャネルでデータシリアライゼーションモードをセットする。デフォルト値はBINARY。

   ```
   BINARY
   BINARY_UTF8
   JSON
   NONE
   ```

 * constraints(MediaConstraints)
     * ローカルメディアストリーム設定オブジェクトを指定。MediaConstraintsは[Android SDK APIリファレンス](https://nttcom.github.io/skyway/docs/#Android-mediaconstraints)に準ずる
 * polling (boolean)
     * サーバポーリングによるユーザリストのチェックを許可する。デフォルト値はtrue。
 * polling_interval (int)
     * ポーリング間隔(msec)を設定する。デフォルト値は3000。
 * debug (DebugLevelEnum)
     * デバッグ情報出力レベルを設定する。デフォルト値はNO_LOGS
 ```
 NO_LOGS ログを表示ない
 ONLY_ERROR エラーだけ表示
 ERROR_AND_WARNING エラーと警告だけ表示
 ALL_LOGS すべてのログを表示
 ```
 * host (String)
     * peerサーバのホスト名。デフォルト値は"skyway.io"
 * port (int)
     * peerサーバのポート番号。デフォルト値は443
 * path (String)
     * peerサーバのpath。デフォルト値は"/"
 * secure (boolean)
     * peerサーバとの接続にTLSを使用する。デフォルト値はtrue
 * config (ArrayList<IceConfig>).
     * STUN/TURNサーバ設定オブジェクトIceConfigのArrayListを設定する。IceConfigは[Android SDK APIリファレンス](https://nttcom.github.io/skyway/docs/#Android-iceconfig)に基づく
 * useSkyWayTurn (boolean)
     * SkyWayのTURNサーバを使用する場合はtrue。SkyWayのTURNを使用する場合は別途、[TURNサーバ使用申請](https://skyway.io/ds/turnrequest)が必要。デフォルト値はtrue


### start()

SkyWayサーバに接続し、peerに接続します。失敗した場合にはerrorイベントが呼び出されます。
```java
multiparty.start();
```

### on(event,callback)

各種イベント発生時のコールバックを設定できます。

* event (MultiPartyEventEnum)
    * 設定するイベント種別を指定
* callback (OnCallback)
    * イベント発生時に実行するコールバックオブジェクトを設定

#### 'MultiPartyEventEnum.OPEN'
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
* SkyWayサーバとのコネクションが確立した際に発生します。
* **peerId** : 現在のウィンドウのid

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
* このウィンドウのvideo/audioストリームのセットアップが完了した際に発生します。
* **src** : キャプチャされたストリーム。
* **id** : 現在のウィンドウのid。

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
* peerのvideo/audioストリームのセットアップが完了した際に発生します。
* **src** : peerのストリーム。
* **id** : peerのid。
* **reconnect** : reconnectメソッドにより再接続された場合はtrueとなる。


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
* peerのスクリーンキャプチャストリームのセットアップが完了した際に発生します。
* **src** : peerのスクリーンキャプチャストリーム。
* **id** : peerのid。
* **reconnect** :reconnectメソッドにより再接続された場合はtrueとなる。

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
* peerのメディアストリームがクローズした際に発生します。
* **peer-id** : peerのid。

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
* peerのスクリーンキャストストリームがクローズした際に発生します。
* **peer-id** : peerのid。

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
* データチャンネルのコネクションのセットアップが完了した際に発生します。
* **id** : peerのid。

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
* peerからメッセージを受信した際に発生します。
* **peer-id** : peerのid。
* **data** : 受信したデータ。

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
* データコネクションがクローズした際に発生します。
* **peer-id** : peerのid。

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
* エラーが起きたら発生します。
* **error** : 発生したErrorオブジェクト。[Android SDK APIリファレンスのPeerErrorクラス](https://nttcom.github.io/skyway/docs/#Android-peererror)に基づく

### mute(boolean video,boolean audio)
自分の映像と音声をミュートすることができます。

void mute(boolean video,boolean sudio);

* video (boolean)
    * true:映像を停止 false:映像を送出
* audio (boolean)
    * true:音声を停止 false:音声を送出

```java
multiparty.mute(true,true);
```

### removePeer(String peerId)
peerのメディアストリームとデータストリームをクローズします。

boolean removePeer(String peerId);

* peerId (String)
    * 切断するリモートピアIDを指定

```java
multiparty.removePeer({peer-id});
```

### send(Object data)

boolean send(Object data)

peerにデータを送信します。

* data (Object)
    * 送信するデータ

```java
String message ＝ ”Hello”；
multiparty.send(MESSAGE);
```


### close()

コネクションを全て切断します。

boolean close();

```java
multiparty.close();
```

### listAllPeers(OnCallback() callback)
接続しているpeerのidを取得します。

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
