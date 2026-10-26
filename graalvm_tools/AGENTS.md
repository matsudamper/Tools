# graalvm_tools

## 適用するルール
- リポジトリルートの `docs/agent-kotlin.md` と `docs/agent-gradle.md` に従う
- これらの言語・ビルド固有ルールは `graalvm_tools/` 内に適用する

## ビルド
```sh
JAVA_HOME=$JAVA_17_HOME ./gradlew nativeCompile
```
