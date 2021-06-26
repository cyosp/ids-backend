# IDS / back-end

## Build

### Bootable JAR

IDS back-end can be built into a single bootable JAR with:

`./gradlew bootJar`

Bootable JAR is then in directory: `build/libs`

### Docker image

IDS can also be built into a Docker image

In that case build the bootable JAR and run after:

`./gradlew docker`

Built Docker image name: `cyosp/ids-backend`

## Run

### Bootable JAR

Update `ids.toml` project file if needed and run:

`java -jar build/libs/ids-*.jar`

### Docker container

Replace:
 * `/path/to/ids/data/directory` with your own IDS data folder
 * `/path/to/images/directory` with your own images directory
 * `shared-images` with the directory configured inside `ids.toml`
 * `0.0.0` with the expected IDS version

And run:
```
docker run -v /path/to/ids/data/directory:/data \
    -v /path/to/images/directory:/shared-images \
    -m 150M \
    -p 8080:8080 cyosp/ids-backend:0.0.0
```

*/!\ Both directories must have write permissions for 'other' file system group /!\\*

