# Gitlet Design Document

## Classes and Data Structures
Main
Gitlet
Branch
Commit
- msg
- time
- parent blob
- merge parent
- shaval

stage:

1. staging area directory
2. hashmapping


file blobs are hashed based on the inner contents of files.1



## Algorithms
Stuff



## Persistence

.gitlet
- .commits(DIR)
    - all the commits stored
- .blobmapping(DIR) "
  - for every commit there exists a blob mapping with the same name in this dir
- .blobsStagedAdd(DIR) "add area"
    - all the blobs staged for add
- .blobs(DIR)
  - all the file blobs for commit
- .metadata
  - serialized GItlet object
  - GITLET
- .stagedAdd
  - serialized hashmap for files staged for add
  - ADDSTGMAP
- .stagedRm
  - serialized hashmap for filfes staged for rm
  - RMSTGMAP
- .branchMAP
  - serialized hashmap for branchs
  - 
