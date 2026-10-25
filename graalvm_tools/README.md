```shell
JAVA_HOME=$JAVA_17_HOME ./gradlew nativeCompile
```

## SharePoint(OneDrive)
環境変数 `tenantId` `clientId` `clientSecret` `userObjectId` が必要。

```shell
# ファイルをアップロード
util sharepoint upload <Local FileName> <Remote FileName>

# ディレクトリ配下の追加・変更されたファイルだけアップロード(上書き)
util sharepoint upload-diff ./dist /Backup/dist

# アップロード対象の確認のみ
util sharepoint upload-diff --dry-run ./dist /Backup/dist
```
