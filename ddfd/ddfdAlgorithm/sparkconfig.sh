#!/bin/bash

# Initialize variables
SPARK_HOME=/usr/local/spark
MASTER_NODE=master_node_hostname
THIS_NODE=this_node_hostname
# Replace paths and hostnames with actual values.

# Configuring Spark Master
if [ "${THIS_NODE}" = "${MASTER_NODE}" ]; then
  echo "Configuring as Master"

  cp ${SPARK_HOME}/conf/spark-env.sh.template ${SPARK_HOME}/conf/spark-env.sh
  echo "export SPARK_MASTER_HOST='${MASTER_NODE}'" >> ${SPARK_HOME}/conf/spark-env.sh

  # Start Spark master service
  ${SPARK_HOME}/sbin/start-master.sh
else
  # Configuring the Slaves
  echo "Configuring as Slave Node"

  # Register master in each worker node
  if [ ! -f ${SPARK_HOME}/conf/slaves ]; then # if slaves  file does not exist
      cp ${SPARK_HOME}/conf/slaves.template ${SPARK_HOME}/conf/slaves
  fi
  echo "${MASTER_NODE}" >> ${SPARK_HOME}/conf/slaves

  # Start worker node
  ${SPARK_HOME}/sbin/start-worker.sh spark://${MASTER_NODE}:7077
fi