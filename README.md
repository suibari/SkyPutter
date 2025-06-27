![Image](app/src/main/res/drawable/logo_skyputter.png)

SkyPutter(すかいぷったー)は、BlueSky向けの投稿専用/通知受信可能なクライアントアプリです。

SNSでの情報のインプットが多くて疲れる、けれどリプライや、気持ちのアウトプットはしていたい、そんな人のためのアプリです。

# 機能

以下の機能を持ちます。

* ログイン(セルフホストPDS対応)
* 自動セッション更新
* ポストする(通常ポスト、返信/引用、画像動画添付)
* 自分のポストを見る
* 各種通知のフォアグラウンド取得とデバイス通知(無効化可能)
* 下書き機能

以下の機能はサポートしていません。

* 自分以外のユーザのプロフィールやタイムラインを見る
* ポストについたいいね数、リポスト数を見る
* DM関連

# 使い方
## ログイン画面

アプリ初回起動時とログアウト時はログイン画面が表示されます。

![Image](https://github.com/user-attachments/assets/dd92e8d5-9752-4b88-b585-5fb2eaf3d9f9)

## メイン画面

ログイン後、メイン画面が表示されます。

この画面でポスト投稿を行います。

入力途中のポスト文は他画面に遷移しても保持されます。

ポスト文を入力途中であれば、その文章の下書き保存が行えます。

![Image](https://github.com/user-attachments/assets/bcdb9075-1115-4ff8-83e8-5c992c2f6831)

## 通知画面

通知アイコンをタップすると通知画面に遷移します。

通知一覧を下にスワイプすると手動更新し、全通知が既読になります。

通知一覧最下部に到達すると、さらに通知を読み込みます。

![Image](https://github.com/user-attachments/assets/3e98fb45-2ffd-4fa2-a37e-c6eebf798905)

## ポスト一覧画面

ユーザアイコンをタップすると自分のポスト一覧画面に遷移します。

ポスト一覧を下にスワイプすると最新ポストを読み込みます。

ポスト一覧最下部に到達すると、さらにポストを読み込みます。

ポストを左にスワイプすることで削除できます。

![Image](https://github.com/user-attachments/assets/b09bcd78-5648-4be6-8294-e1df52b6ea86)

## 下書き画面

下書き画面は、メイン画面で1つ以上の下書きデータを保存したときに遷移できます。

下書きを選択すると、その下書きをポスト文に反映し、その下書きデータはリストから削除されます。

下書きを左にスワイプすることで削除できます。

![Image](https://github.com/user-attachments/assets/35e20d2d-c528-46ef-a8b3-b015f38a14ca)

## 設定画面

ユーザアイコンのロングタップで出てくるメニューから遷移できます。

# TIPS

## フォアグラウンドでの通知取得とは？

アプリがタスクトレイにある状態(画像参照)をフォアグラウンド状態と呼びます。

この状態であれば、SkyPutterは通知を定期取得できます。

![Image](https://github.com/user-attachments/assets/75f9b5ab-1d08-480b-8936-ee5bdb98460e)

通知取得開始時は、Androidのサイレント通知が表示されます。

![Image](https://github.com/user-attachments/assets/7de77369-18e5-4c27-9014-ea3ec2a8ddba)

逆に言えば、アプリをタスクトレイからスワイプして終了させると通知取得できなくなります。

この場合はもう一度アプリを再起動すれば通知取得開始します。

フォアグラウンドでの定期取得は設定からオフにすることも可能です。

### フォアグラウンド実行のリスクについて

通常、アプリのフォアグラウンド実行はバッテリー消費、データ通信量消費が大きくなります。

ただすいばりが実機測定した限り、SkyPutterは他アプリと比べても一般的な消費量となっていました。(以下参考)

もし著しい消費量となっていた場合は意図しない現象なので、申し訳ございませんが、すいばりまでご連絡ください。

![Image](https://cdn.bsky.app/img/feed_fullsize/plain/did:plc:uixgxpiqf4i63p6rgpu7ytmx/bafkreih3tci5kxarvbfjd2e3ywnd5jcatb6zjblk3i2vmyatmtonzvv4te@jpeg)
![Image](https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:uixgxpiqf4i63p6rgpu7ytmx/bafkreifrdcub4ai3shihpcyyjodjy2zzcutwbitjlcb2ziaexwhnegvzi4@jpeg)

# 謝辞

本アプリはKotlin向けBlueskyライブラリ[kbsky](https://github.com/uakihir0/kbsky)を使用しています。

また、リリースにあたりBlueskyでたくさんのユーザにテスト協力していただきました。

この場を借りて感謝申し上げます！
