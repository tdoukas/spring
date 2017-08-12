# spring

Spring embedder algorithms with graphical visualization.

## About

This program features modifications of the classic spring embedder algorithm of Fruchterman and Reingold
that allow the introduction of rigid paths during computation.
See [presentation.pdf](presentation.pdf) for an introduction and some theoretical background.

This program is part of [my B.Sc. thesis at FernUni Hagen](http://www.fernuni-hagen.de/ti/lehre/abschlussarbeiten/spring_embedder.shtml). The full thesis (in german) is available upon request.

## Features

- Graphic visualization of standard spring embedder algorithms for educational purposes
- Experiment with various embedder settings and algorithms
- Include your own graphs in one of three formats (matrixMarket coordinates, Rome, GraphML)
- Save drawings as SVG and PNG

## Running

This is a maven project that builds an executable jar. In the main directory, enter
```
mvn clean package
```
and then
```
java -jar target/*.jar
```

## Copyright

The program itself was written by Theo Doukas.
You are free to use any part of the source code in any way you like.

The graph libraries carry the copyright of their respective owners:

- The Graphs in the [matrixData](matrixData/) directory were taken from http://math.nist.gov/MatrixMarket/ .
- The [Rome Graph Library](rome-lib.zip) used to be part of the Graph Drawing Toolkit, see http://www.dia.uniroma3.it/~gdt/gdt4/index.php .
- The [GraphML graphs](graphml.zip) were taken from http://www.graphdrawing.org.

While not an actual part of the program, these libraries are included in this repository in the proper format required by the program for convenience.