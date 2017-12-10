# セットアップ

## 事前に

### インストールするもの

* Java SDK 8
* sbt 1.0
* dockerおよびdocker-compose

### 設定などが必要なもの

* elasticsearch basic licenseの確保

https://www.elastic.co/jp/subscriptions

https://medium.com/veltra-engineering/elastic-stack-basic-license-98344bbbeac1

フォルダ **docker/es/**　に **license.json** という名前でファイルを置くと、dockerファイル作成時（後述）に自動で
licenseファイルが

* （Linux環境でdockerを動かす場合）カーネルパラメータの調整

```bash
sudo sysctl -w vm.max_map_count=262144
sudo chown -R 1000 docker/volumes
```

* ライセンスファイルをインストール

```
# elasticsearchのdockerコンテナのみ起動
cd docker
docker-compose up -d es
 
# 初回は初期化作業に時間がかかりますので、しばらく待ってから
# 以下のコマンドでライセンスファイルをインストール

docker-compose exec es curl -XPUT -u elastic 'http://localhost:9200/_xpack/license?acknowledge=true' -H "Content-Type: application/json" -d "@/usr/share/elasticsearch/license.json"



```

# 起動
## 開発モードで起動する

```

# play起動
export SBT_OPTS=-javaagent:./lib/aspectjweaver-1.8.10.jar
sbt run

# fluentd-elasticsearch-kibana-head起動
cd docker
docker-compose up -d
```

## productionモードで起動する

```
sbt stage
cd docker
docker-compose up -f docker-compose.yml -f docker-compose-app.yml -d
```
