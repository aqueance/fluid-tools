#!/usr/bin/env bash -e

function die {
    echo "$1" >&2
    exit -1;
}

BRANCH=$(git branch | grep '*' | cut -d\  -f2)
[ "$BRANCH" == "master" ] || die "Deployment aborted on branch ${BRANCH}"

function groupId {
    local POM=pom.xml

    [ -r ${POM} ] || die "Root POM not found: ${POM}."

    # Use awk to parse the root POM for the groupId and artifactId
    local POM_FILTER=pom-filter.awk
    trap "rm -f ${POM_FILTER}" EXIT

    # some local variables for the awk script
    local MV=modelVersion GP=groupId AF=artifactId

    cat >${POM_FILTER} <<END_SCRIPT
{
    FS = "<|>";

    if (\$2 == "${MV}") { ${MV} = 1; ${GP} = ""; ${AF} = ""; next; }
    if (${MV} == 1 && \$2 == "${GP}") { ${GP} = \$3; next; }
    if (${MV} == 1 && \$2 == "${AF}") { ${AF} = \$3; next; }
    if (${GP} != "" && ${AF} != "") { print ${GP} "." ${AF}; exit; }
}
END_SCRIPT

    # Do the extraction
    awk -f ${POM_FILTER} ${POM}
}

function version {
    local POM=pom.xml

    [ -r ${POM} ] || die "Root POM not found: ${POM}."

    # Use awk to parse the root POM for the version
    local POM_FILTER=pom-filter.awk
    trap "rm -f ${POM_FILTER}" EXIT

    # some local variables for the awk script
    local MV=modelVersion VS=version

    cat >${POM_FILTER} <<END_SCRIPT
{
    FS = "<|>";

    if (\$2 == "${MV}") { ${MV} = 1; ${VS} = ""; next; }
    if (${MV} == 1 && \$2 == "${VS}") { ${VS} = \$3; next; }
    if (${VS} != "") { print ${VS}; exit; }
}
END_SCRIPT

    # Do the extraction
    awk -f ${POM_FILTER} ${POM}
}

function project {
    local POM=pom.xml

    [ -r ${POM} ] || die "Root POM not found: ${POM}."

    # Use awk to parse the root POM for the name and version
    local POM_FILTER=pom-filter.awk
    trap "rm -f ${POM_FILTER}" EXIT

    # some local variables for the awk script
    local MV=modelVersion NM=name VS=version

    cat >${POM_FILTER} <<END_SCRIPT
{
    FS = "<|>";

    if (\$2 == "${MV}") { ${MV} = 1; ${NM} = ""; ${VS} = ""; next; }
    if (${MV} == 1 && \$2 == "${NM}") { ${NM} = \$3; next; }
    if (${MV} == 1 && \$2 == "${VS}") { ${VS} = \$3; next; }
    if (${NM} != "" && ${VS} != "") { print ${NM} " " ${VS}; exit; }
}
END_SCRIPT

    # Do the extraction
    awk -f ${POM_FILTER} ${POM}
}

GROUP_ID=$(groupId)
[ -z "${GROUP_ID}" ] && die "Group ID could not be identified."

PROJECT=$(project)
[ -z "${PROJECT}" ] && die "Project could not be identified."

VERSION=$(version)
[ -z "${VERSION}" ] && die "Version could not be identified."

COMMIT=$(git rev-parse HEAD)
[ -z "${COMMIT}" ] && die "Commit could not be identified."

GROUP_DIR=$(echo ${GROUP_ID} | sed -e 's/\./\//g')

M2="${HOME}/.m2"
REPO=repository
JAVADOC=apidocs
LOCAL_REPO="${M2}/${REPO}"
CATALOG_FILE=archetype-catalog.xml

STAGING=staging
STAGING_REPO=${STAGING}/${REPO}
STAGING_JAVADOC=${STAGING}/${JAVADOC}

function prepare {
    [ -d ${STAGING} ] && [ ! -d ${STAGING}/.git ] && die "${STAGING} is not a Git repository."
    [ -d ${STAGING} ] || git clone git@github.com:aqueance/maven.git ${STAGING}
}

function artifacts {
    echo "Generating artifacts to ${STAGING_REPO}"

    # We are NOT using the deploy goal as it adds timestamp prefixes and such (Reproducible snapshot builds? Haha, good one.)
    #mvn deploy -Ddistribution "-DaltDeploymentRepository=staging::default::file://$(pwd)/${STAGING_REPO}"

    # Install the artifacts with checksum
    mvn -q clean install -Ddistribution -DcreateChecksum=true

    [ -d ""${LOCAL_REPO}/${GROUP_DIR}"" ] || die "Artifacts not found: ${LOCAL_REPO}/${GROUP_DIR}"

    # Copy the artifacts to the staging area
    (cd "${M2}"; tar cJf - "${REPO}/${GROUP_DIR}") | (cd ${STAGING}; tar xJf -)
}

function archetype_fragment {
    echo "${STAGING}/${1}-${CATALOG_FILE}-fragment"
}

function archetypes {
    [ -r "${M2}/${CATALOG_FILE}" ] || die "Archetype catalog not found: ${M2}/${CATALOG_FILE}."

    echo "Generating archetype catalog in ${STAGING}"

    # We are NOT using the archetype plugin as it fails to set the archetype description
    #mvn -N archetype:crawl -Drepository=${STAGING_REPO}

    # Use awk to parse the local archetype catalog and extract the archetypes by Fluid Tools
    local CATALOG_FILTER=catalog-filter.awk
    local PROLOGUE_FILTER=prologue-filter.awk
    local EPILOGUE_FILTER=epilogue-filter.awk
    trap "rm -f ${CATALOG_FILTER} ${PROLOGUE_FILTER} ${EPILOGUE_FILTER}" EXIT

    # some local variables for the awk script
    local ATS=archetypes AT=archetype GP=groupId AF=artifactId VS=version DS=description AGP=${GROUP_ID}.archetypes

    cat >${CATALOG_FILTER} <<END_SCRIPT
{
    FS = "<|>";

    if (\$2 == "${AT}") { ${AT} = 1; I = \$1; II = I "  "; ${GP} = ""; AF = ""; ${VS} = ""; ${DS} = ""; next; }
    if (${AT} == 1 && \$2 == "${GP}") { ${GP} = \$3; next; }
    if (${AT} == 1 && \$2 == "${AF}") { ${AF} = \$3; next; }
    if (${AT} == 1 && \$2 == "${VS}") { ${VS} = \$3; next; }
    if (${AT} == 1 && \$2 == "${DS}") { ${DS} = \$3; next; }
    if (\$2 == "/${AT}") { ${AT} = 0; if (${GP} == "${AGP}") { print I "<${AT}>\n" II "<${GP}>" ${GP} "</${GP}>\n" II "<${AF}>" ${AF} "</${AF}>\n" II "<${VS}>" ${VS} "</${VS}>\n" II "<${DS}>" ${DS} "</${DS}>\n" I "</${AT}>"; } next; }
}
END_SCRIPT

    # Extract the XML prologue before the archetype tags
    cat >${PROLOGUE_FILTER} <<END_SCRIPT
{
    FS = "<|>";

    if (\$2 == "${AT}") { ${AT} = 1; exit; }

    print;
}
END_SCRIPT

    # Extract the XML epilogue after the archetype tags
    cat >${EPILOGUE_FILTER} <<END_SCRIPT
{
    FS = "<|>";

    if (\$2 == "/${ATS}") ${ATS} = 1;
    if (${ATS} != 1) next;

    print;
}
END_SCRIPT

    local SOURCE="${M2}/${CATALOG_FILE}"
    local TARGET="${STAGING}/${CATALOG_FILE}"

    awk -f ${CATALOG_FILTER} "${SOURCE}" >"$(archetype_fragment "${GROUP_ID}-${VERSION}")"    # Extract this project's archetypes
    awk -f ${PROLOGUE_FILTER} "${SOURCE}" >"${TARGET}"                          # Add the prologue
    cat $(archetype_fragment \*) >>"${TARGET}"                                  # Add all archetypes
    awk -f ${EPILOGUE_FILTER} "${SOURCE}" >>"${TARGET}"                         # Add the epilogue
}

function javadoc {
    local APIDOCS="${STAGING_JAVADOC}/${GROUP_ID}-${VERSION}"

    echo "Generating API docs to ${APIDOCS}"

    # Generate the API documentation
    mvn -q javadoc:aggregate

    mkdir -p "${APIDOCS}"

    (cd target/site/apidocs; tar cJf - .) | (cd "${APIDOCS}"; tar xJf -)

    # Record the project ID to which this API documentation belongs
    local PROJECT_ID="${PROJECT} $(date)"
    local PROJECT_ID_FILE=project-id.txt
    echo "${PROJECT_ID}" >"${APIDOCS}/${PROJECT_ID_FILE}"

    # Generate the index.html for all generated API docs
    cat >"${STAGING_JAVADOC}/index.html" <<END_FILE
<!doctype html>
<head>
    <title>API documentation</title>
    <link rel="stylesheet" href="../css/page.css"/>
</head>
<body>
$(cd "${STAGING_JAVADOC}"; for DIR in */; do [ -r "${DIR}/${PROJECT_ID_FILE}" ] && echo "<h4><a href=\"${DIR}\">$(cat "${DIR}/${PROJECT_ID_FILE}")</a><h4>"; done)
</body>
END_FILE
}

function upload {
    echo "Committing changes"

    local COMMENT="${PROJECT} ${COMMIT} $(date)"
    (cd ${STAGING_REPO}; git add -A; git commit -m "${COMMENT}")
}

prepare
artifacts
javadoc
archetypes
upload
echo "Staging done"
