# Asp-it: ASP based information terms generator

Prototype for ASP based generator for Information Terms of constructive
description logic *ELc*.

*The prototype with included example data sources is a proof of concept of our
research idea and is intended to be used only to demonstrate and evaluate our
work. Please contact the authors for any other need.*

## Installation

The latest binary release of Asp-it (1.0.1) can be found at [dkm.fbk.eu/resources/asp-it/asp-it.zip](https://dkm.fbk.eu/resources/asp-it/asp-it.zip).  
See also [Asp-it GitHub releases page](https://github.com/dkmfbk/asp-it/releases) for current and previous releases.  

The [source code](https://github.com/dkmfbk/asp-it) is distributed as a Maven project: binaries can be built executing `mvn assembly:assembly` from the main project directory. 

## Requirements

 * DLV 2012-12-17 (or newer) [http://www.dlvsystem.com/dlv/](http://www.dlvsystem.com/dlv/)  
   For ease of use, it is preferrable to install a copy of the DLV executable in 
   `/localdlv/dlv`, the directory used by default as DLV path by the prototype

 * Java runtime version 1.7 (or greater)
 * Windows, Linux or Mac OS X operating system 

## Usage 

```
Usage: asp-it <input-ontology-file> [<options>]

<input-ontology-file>
  OWL file containing the ontology for which IT have to be computed.

Example: asp-it input.n3

Options:
 -v: verbose (prints more information about loading and computing process)
 -out <output-file>: specifies the path to the output ontology file 
                     (default: <input-ontology-file>-out.n3)
 -lp <program-file>: output the result of rewriting to the specified file 
                     (default: false)
 -dlv <dlv-path>   : specifies the path to the DLV executable 
                     (default: localdlv/dlv)
```

## Demo

Refer to `/demo/DEMO_README.txt` for usage examples.

## Known limitations 

- Input ontologies have to be in EL: currently, no check is applied on the input OWL profile.
            
- Input ontology has to import (`owl:import`) the schema for ELc primitives (provided in `/schemas/elc-schema.n3`).
      
- Input ontologies have to be valid OWL ontologies: as an effect, all of the used symbols (classes and properties) have to be specified.