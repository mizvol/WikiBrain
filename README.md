# WikiBrain
**UPDATE:** the latest version of implementation for [Anomaly detection in Web and Social Networks](https://arxiv.org/abs/1901.09688) paper is available [here](https://github.com/epfl-lts2/sparkwiki/blob/master/src/main/scala/ch/epfl/lts2/wikipedia/PeakFinder.scala). Also, we have implemented a very intuitive and concise (but inefficient) Python implementation for practitioners to provide overall understanding of the algorithm. More details [here](https://github.com/mizvol/anomaly-detection).

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
