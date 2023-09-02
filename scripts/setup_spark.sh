#!/bin/bash

spark-class org.apache.spark.deploy.master.Master --host localhost --port 7077 --webui-port 8080 &

sleep 5
export SPARK_WORKER_DIR=.

# Loop to start eight Spark workers
for ((i=1; i<=8; i++)); do
    spark-class org.apache.spark.deploy.worker.Worker spark://localhost:7077 --cores 1 &
done

# Wait for the workers to start
sleep 5

echo "All Spark workers started."
