#!/bin/bash

# Set your Spark master IP and desired number of cores
export MASTER_IP=spark://localhost:7077
export CORES=1

# Set your input file path and separator
inputPath=$1
seperator=$2

export tempPath=tempPath.tmp
export numAttributes=$(awk -F "$seperator" '{print NF; exit}' "$3")

export numPartitions=8

# Submit Spark job with specified configuration
spark-submit --master $MASTER_IP \
--executor-memory 5G \
--driver-memory 5G \
--executor-cores $CORES \
--conf spark.driver.maxResultSize=20g \
--conf spark.memory.fraction=0.3 \
--conf spark.memory.storageFraction=0.5 \
--conf spark.shuffle.spill.compress=true \
--class pasa.bigdata.nju.smartfd.main.Main \
algorithms/SparkFD-assembly-1.0.jar \
--inputFilePath "${inputPath}" --tempFilePath "${tempPath}" --numAttributes ${numAttributes} --outputFilePath "${inputPath}_results"
