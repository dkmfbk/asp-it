================================================================================
      Asp-it: ASP based information terms generator - DEMO README
================================================================================

= INTRODUCTION = 
The files presented in this demo represent an implementation for the example
introduced in the Asp-it reference paper [1]. Their goal is to demonstrate
the functionality of the prototype and provide an intuitive 
interpretation for its output.

By running the shell file in this folder ("food-and-wines-demo.bat" or ".sh"), 
Asp-it is executed using the corresponding RDF file "food-and-wines.n3".

The resulting annotated output ontology is saved to "out-ontology.n3".
The DLV program generated in the rewriting is saved to "out-program.dlv".

--------------------------------------------------------------------------------
= EXAMPLE: FOOD AND WINES =

We want to compute the information terms for a set of axioms describing the 
suggested pairings of foods and wines.
In the ABox we have the following members of classes of foods, wines and 
wine colors:

    :meat a :Food .    :barolo a :Wine .        :red a :Color .  
    :fish a :Food .    :chardonnay a :Wine .    :white a :Color .
                       :teroldego a :Wine .       

Role assertions provide correct pairing of food and colors and link each wine to
its color:

    :red :isColorOf :barolo .         :meat :goesWith :red .
    :red :isColorOf :teroldego .      :fish :goesWith :white .  
    :white :isColorOf :chardonnay .

The first TBox axiom (Ax1) defines that each food has some associated color and
corresponds to:

    \forall_Food \exists goesWith Color
    
The second TBox axiom (Ax2) defines that each color is color of some wine and
corresponds to:

    \forall_Color \exists isColorOf Wine
    
Last Tbox axiom (Ax3) combines the previous axioms and defines the correct
associations of food to wines:
    
    \forall_Food \exists goesWith ( Color \and \exists isColorOf Wine )

By executing Asp-it, we can compute the information terms for these formulas,
corresponding to "answers" to previous axioms.

By the resulting "hasIT" assertions in the output ontologies, for (Ax1) we 
obtain these associations:

   ["fish", ["white", tt]]   ["meat", ["red", tt]]

representing the IT function:

   [ "fish" -> ["white", tt], "meat" -> ["red", tt] ]

For (Ax2) we obtain the associations:
 
   ["red", ["barolo", tt]]
   ["red", ["teroldego", tt]]
   ["white", ["chardonnay", tt]]

corresponding to two functions:

   [ "red " -> ["barolo", tt], "white" -> ["chardonnay", tt] ]
   
   [ "red" -> ["teroldego", tt], "white" -> ["chardonnay", tt] ]
   
Finally, for (Ax3) we obtain the associations:

   ["fish", ["white", [tt, ["chardonnay", tt]]]]
   ["meat", ["red", [tt, ["barolo", tt]]]]
   ["meat", ["red", [tt, ["teroldego", tt]]]]

corresponding to the two functions:

   [ "fish" -> ["white", [tt, ["chardonnay", tt]]], 
     "meat" -> ["red", [tt, ["barolo", tt]]] ]
     
   [ "fish" -> ["white", [tt, ["chardonnay", tt]]], 
     "meat" -> ["red", [tt, ["teroldego", tt]]] ]     

--------------------------------------------------------------------------------
= REFERENCES =

[1] Bozzato, L.: 
    ASP Based Generation of Information Terms for Constructive EL.

================================================================================