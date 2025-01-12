# Implementation details

## Extraction
 -  Directly done in the code for portability
 -  The projects are extracted via GitHub

---
## Releases
 -  Extracted from Jira
 -  If Jira doesn't have the release date, it's searched via GitHub
 -  Only GitHub tagged releases are kept, tag is needed for complexity metrics extraction
 -  The project is configurable for using the brand-new approach of fake releases: in order to avoid wasting information,
    it allows to create fake releases as long as the average length of project's releases. In this way, if there are commits
    and tickets created after the last available release, they won't be discarded
 -  Removed releases without commits
 -  Removed releases without date

---
## Commits
 -  Extracted from all branches of the GtHub repository
 -  Assumed a commit belongs to the first release after its creation date
 -  Assumed a commit belongs to the ticket referred in its message
 -  Assumed a commit without ticket-id in its commit message doesn't belong to any ticket, so it's discarded


---
## Tickets
 -  Extracted from Jira
 -  Assumed Jira ticket dates are correct
 -  Assumed Jira tickets labeled as defect are truly defects
 -  Checked coherence between OV, FV and AVs
 -  Removed tickets without commits


---
## Proportion
 -  Method for computing the IV of the tickets without it.
 -  Project configurable to use both the incremental proportion and the new proportion discussed during lectures
 -  New proportion: from the point-of-view of the i-th release, the proportion is computed using the available IVs
    of all the tickets before the i-th release, and then it's used to compute the IV of all the tickets without it


---
## Complexity metrics
 -  Complexity metrics extracted using com.github.mauricioaniche.ck module. It extracts all complexity metrics,
    both class-level and method-level, even though only 4 class-level metrics has been considered:cbo, fan-in,
    fan-out and the number of public methods
 -  The cbo metric (Coupling between objects) is a class-level metric: it counts the number of dependencies a class has
 -  The fan-in metric is a class-level metric: counts the number of input dependencies a class has
    (the number of classes that reference a particular class)
 -  The fan-out metric is a class-level metric: counts the number of output dependencies a class has
    (the number of other classes referenced by a particular class)
 -  The public method quantity metric is a class-level metric: it counts the number of public methods of a class


---
## LOC metrics
 -  LOC class-level metrics extracted are LOC removed, added and churn: for each of there metric their average,
    max value and actual value has been computed
 -  Other class-level metrics extracted are size, number of revisions, number of defect fixes, number of authors


---
## Datasets
 -  Taken into account only the first half releases of the n releases for the walk forward process
 -  At the i-th release, the i-th one is the testing subject for test set, the previous are for training set
 -  At the i-th release, the training set with the releases [0, i-1] is computed with all the information until the (i-1)-th release
 -  At the i-th release, the testing set composed by the i-th release is computed with all the information until present, so until he n-th release
 -  The first release is skipped, so the first training set uses the 1th release and the first test set uses the 2nd release


---
## Weka
 -  The implemented weka classifiers use feature selection and cost-sensitive classifiers
 -  The project is configurable for using sampling during training
 -  The project is configurable for setting the weights of the cost-sensitive classifiers