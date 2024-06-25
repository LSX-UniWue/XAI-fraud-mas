
This includes a quick setup guide for running the simulation in IntelliJ IDEA.

1. Open Folder in IntelliJ

2. Right Click on Module `SCM_ERP_MAS` in Project Tree &rarr; Open Module Settings

3. Go to Dependencies:

    - SDK &rarr; `OpenJDK version 19.0.1`
<!-- -->

4. Go to Sources:

    - Mark Folder `resources` as resource folder
<!-- -->

5. Click OK

6. Go to Run &rarr; Edit Configurations &rarr; Add new &rarr; Application:

    - Main Class &rarr; `jade.Boot`

    - Program arguments &rarr; `-agents Delivery:Agents.DL;ERP_L:Agents.ERP_L;Markets:Agents.Markets;Make:Agents.MK;Source:Agents.SC;GUIController:Agents.GUIController`

    - If JADE management console needs to be accessed add `-gui` infront of the program arguments

7. Click OK

8. Click Run
