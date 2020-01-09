**UPDATE:** the latest version of implementation for [Anomaly detection in Web and Social Networks](https://arxiv.org/abs/1901.09688) paper is available [here](https://github.com/epfl-lts2/sparkwiki/blob/master/src/main/scala/ch/epfl/lts2/wikipedia/PeakFinder.scala). 

To run the code, you will need to pre-process Wikipedia pagedumps and pagecounts. To do that, follow the [instructions]().

Once you have pre-processed the dumps, you can run the algorithm using `spark-submit` from the [sparkwiki repository](https://github.com/epfl-lts2/sparkwiki) that you've used for dumps pre-processing. See an example of the command below:

`spark-submit --class ch.epfl.lts2.wikipedia.PeakFinder --master 'local[*]' --executor-memory 30g --driver-memory 30g --packages org.rogach:scallop_2.11:3.1.5,com.datastax.spark:spark-cassandra-connector_2.11:2.4.0,com.typesafe:config:1.3.3,neo4j-contrib:neo4j-spark-connector:2.4.0-M6,com.github.servicenow.stl4j:stl-decomp-4j:1.0.5,org.apache.commons:commons-math3:3.6.1,org.scalanlp:breeze_2.11:1.0 target/scala-2.11/sparkwiki_<VERSION OF SPARKWIKI>.jar --config config/peakfinder.conf --language en --parquetPagecounts --parquetPagecountPath <PATH TO THE OUTPUT OF PagecountProcessor> --outputPath <PATH WHERE YOU'LL HAVE RESULTING GRAPHS WITH ANOMALIES>`

Parameters explained:

```
spark-submit 
--class ch.epfl.lts2.wikipedia.PeakFinder 
--master 'local[*]' [use all available cores]
--executor-memory [amount of RAM allocated for executor (30% of available RAM)] 
--driver-memory [amount of RAM allocated for driver (40% of available RAM)]
--packages  org.rogach:scallop_2.11:3.1.5,
            com.datastax.spark:spark-cassandra-connector_2.11:2.4.0,
            com.typesafe:config:1.3.3,
            neo4j-contrib:neo4j-spark-connector:2.4.0-M6,
            com.github.servicenow.stl4j:stl-decomp-4j:1.0.5,
            org.apache.commons:commons-math3:3.6.1,
            org.scalanlp:breeze_2.11:1.0 target/scala-2.11/sparkwiki_<VERSION OF SPARKWIKI>.jar
--config [path to config file where you specify parameters of the algorithm]
--language [language code. You can choose any language code but you should have a graph of a corresponding language edition of Wikipedia]
--parquetPagecounts
--parquetPagecountPath [path to the output files of ch.epfl.lts2.wikipedia.PagecountProcessor]
--outputPath [output path where you will have your graphs with anomalous pages]
```

Also, we have implemented a very intuitive and concise (but inefficient) Python implementation for practitioners to provide overall understanding of the algorithm. More details [here](https://github.com/mizvol/anomaly-detection).

# WikiBrain

Implementation of the graph learning algorithm presented in [Wikipedia graph mining: dynamic structure of collective memory](https://arxiv.org/abs/1710.00398). The learning algorithm is inspired by the Hebbian learning theory.

We also reported the results with interactive graph visualizations in an [accompanying blog post](http://blog.miz.space/research/2017/08/14/wikipedia-collective-memory-dynamic-graph-analysis-graphx-spark-scala-time-series-network/).

## Dataset
To reproduce the experiments, download the dataset from [![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.886951.svg)](https://doi.org/10.5281/zenodo.886951).

Clone this project and extract the downloaded .zip files to `/src/main/resources/` folder.

Change `PATH_RESOURCES` in [`Globals.scala`](https://github.com/mizvol/WikiBrain/blob/master/src/main/scala/ch/epfl/lts2/Globals.scala) to the path to this project on your computer.

If you want to reproduce just a part of the experiments, download pre-processed data from [here](https://drive.switch.ch/index.php/s/vEM6cTgft5MUK56) and unzip the files into `PATH_RESOURCES`. This should be enough to run most of the scripts in this repository.

## Runing the experiments
Open [`WikiBrainHebbStatic.scala`](https://github.com/mizvol/WikiBrain/blob/master/src/main/scala/WikiBrainHebbStatic.scala) and run the code (Shift+F10 in Intellij Idea).

You may have to change your Spark configuration according to RAM availability on your computer.

``` scala
val spark = SparkSession.builder
  .master("local[*]") // use all available cores
  .appName("Wiki Brain")
  .config("spark.driver.maxResultSize", "20g") // change this if needed
  .config("spark.executor.memory", "50g") // change this if needed
  .getOrCreate()
 ```

## Resulting graphs
You will find the resulting graph `graph.gexf` in `PATH_RESOURCES`. This file can be opened in [Gephi](https://gephi.org/).
