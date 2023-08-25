spark-submit --jars algorithms/libs/lucene-core-4.5.1.jar,algorithms/libs/fastutil-6.1.0.jar \
--class TaneMain --master spark://localhost:7077 --executor-memory 32G --driver-memory 32G \
--executor-cores 1 algorithms/distributed-tane-new-spark2-1.0.jar \
file:///$(pwd)/$1 8 8