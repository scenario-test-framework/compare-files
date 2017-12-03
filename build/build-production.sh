#!/bin/bash
# --------------------------------------------------------------------------------
# production build
# --------------------------------------------------------------------------------
cd $(cd $(dirname $0); cd ..; pwd;)

# --------------------------------------------------
# 設定
# --------------------------------------------------
readonly NAME_PROJ="$(basename $(pwd))"
readonly DIR_DEPLOY="./deploy"
readonly DIR_DOCS="./docs"
readonly PROFILE="production"

readonly SONAR_URL="https://sonarcloud.io"
readonly SONAR_ORGANIZATION="suwa-sh-github"
readonly SONAR_EXCLUDES="src/test/**,**/classification/**,**/dto/**,**/exception/**,**/*Const.java"

# --------------------------------------------------
# build
# --------------------------------------------------
echo --------------------------------------------------
echo  配布ディレクトリ初期化
echo --------------------------------------------------
if [ -d ${DIR_DEPLOY} ]; then
    rm -fr ${DIR_DEPLOY}
fi
mkdir -p ${DIR_DEPLOY}


echo
echo --------------------------------------------------
echo  build
echo --------------------------------------------------
if [[ "${SONAR_TOKEN}x" = "x" ]]; then
  echo "SONAR_TOKEN が定義されていません。sonar解析をスキップします。"
  readonly CMD_BUILD="mvn clean package -P ${PROFILE} -DPID=$$"
  echo ${CMD_BUILD}
  ${CMD_BUILD}

else
  readonly CMD_BUILD="mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar -P ${PROFILE} -DPID=$$"
  echo ${CMD_BUILD}
  ${CMD_BUILD}                                                                                     \
    -Dsonar.host.url=${SONAR_URL}                                                                  \
    -Dsonar.organization=${SONAR_ORGANIZATION}                                                     \
    -Dsonar.login=${SONAR_TOKEN}                                                                   \
    -Dsonar.exclusions="${SONAR_EXCLUDES}" 2>&1 | tee -a ${PATH_LOG} 2>&1
fi

ret_code=$?
if [ ${ret_code} -ne 0 ]; then
    echo "maven buildでエラーが発生しました。" >&2
    exit 1
fi

echo
echo --------------------------------------------------
echo 配布用ファイルの収集
echo --------------------------------------------------
mv target/${NAME_PROJ}*.tar.gz ${DIR_DEPLOY}

# 結果表示
echo "${DIR_DEPLOY}"
ls -l "${DIR_DEPLOY}"

echo
echo --------------------------------------------------
echo 公開用ファイルの収集
echo --------------------------------------------------
rm -fr ${DIR_DOCS}/site
mv target/site/ ${DIR_DOCS}/

# github.io で表示できるように、jacoco report の .resource へのリンクを resource に置換
find ${DIR_DOCS}/site/jacoco -type f -name '*.html'                               |
xargs -I{} bash -c 'cat {} | sed -e "s|\.resources/|resources/|g" > {}.tmp'
find ${DIR_DOCS}/site/jacoco -type f -name '*.html.tmp'                           |
xargs -I{} bash -c 'filename={}; mv -f ${filename} ${filename%.*}'
mv ${DIR_DOCS}/site/jacoco/.resources/ ${DIR_DOCS}/site/jacoco/resources/

# 結果表示
echo "${DIR_DOCS}/site"
ls -l "${DIR_DOCS}/site"

exit 0
