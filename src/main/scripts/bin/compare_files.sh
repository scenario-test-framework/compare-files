#!/bin/bash
#===================================================================================================
#
# Compare Files
#
#===================================================================================================
#---------------------------------------------------------------------------------------------------
#  定数
#---------------------------------------------------------------------------------------------------
# リターンコード
readonly RETCODE_SUCCESS=0
readonly RETCODE_WARN=3
readonly RETCODE_ERROR=6

# Javaリターンコード ※mappingなし
readonly JAVA_RETCODE_SUCCESS=0
readonly JAVA_RETCODE_WARN=3
readonly JAVA_RETCODE_ERROR=6


#---------------------------------------------------------------------------------------------------
#  主処理
#---------------------------------------------------------------------------------------------------
dir_before=`pwd`

# Java用環境変数設定
cd `dirname $0`/..
classpath=${COMPAREFILES_CLASSPATH}:${classpath}:.:`pwd`/config:`pwd`/lib/*:

# Java呼出し
cd ${dir_before}
java ${COMPAREFILES_JAVA_OPT} -cp ${classpath} -DPID=$$ me.suwash.tools.comparefiles.main.CompareFiles "$@"
java_return_code=$?

# 実行結果確認
if [ ${java_return_code} = ${JAVA_RETCODE_WARN} ]; then
    # warn
    exit ${RETCODE_WARN}

elif [ ${java_return_code} = ${JAVA_RETCODE_ERROR} ]; then
    # error
    exit ${RETCODE_ERROR}

else
    # success
    exit ${RETCODE_SUCCESS}
fi
