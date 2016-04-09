================================================================================
  Asp-it: ASP based information terms generator - README.txt
================================================================================

Prototype for ASP based generator for Information Terms of constructive
description logic ELc.

*The prototype with included example data sources is a proof of concept of our
research idea and is intended to be used only to demonstrate and evaluate our
work. Please contact the authors for any other need.*

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
= USAGE =

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

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
= REQUIREMENTS =

 - DLV 2012-12-17 (or newer) [http://www.dlvsystem.com/dlv/]
   For ease of use, it is preferrable to install a copy of the DLV executable in 
   "/localdlv/dlv", the directory used by default as DLV path by the prototype

 - Java runtime version 1.7 (or greater)
 - Windows, Linux or Mac OS X operating system 

- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
= KNOWN LIMITATIONS =

- [!] Input ontologies have to be in EL: currently, no check is applied on
      the input OWL profile.
            
- [!] Input ontology has to import (owl:import) the 
      schema for ELc primitives (provided in /schemas/elc.n3).
      
- [ ] Input ontologies have to be valid OWL ontologies: as an effect, all of the
      used symbols (classes and properties) have to be specified.
     
================================================================================