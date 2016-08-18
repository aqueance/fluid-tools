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

    cat >${POM_FILTER} <<FILTER_END
{
    FS = "<|>";

    if (\$2 == "${MV}") { ${MV} = 1; ${GP} = ""; ${AF} = ""; next; }
    if (${MV} == 1 && \$2 == "${GP}") { ${GP} = \$3; next; }
    if (${MV} == 1 && \$2 == "${AF}") { ${AF} = \$3; next; }
    if (${GP} != "" && ${AF} != "") { print ${GP} "." ${AF}; exit; }
}
FILTER_END

    # Do the extraction
    awk -f ${POM_FILTER} ${POM}
}

function project {
    local POM=pom.xml

    [ -r ${POM} ] || die "Root POM not found: ${POM}."

    # Use awk to parse the root POM for the groupId and artifactId
    local POM_FILTER=pom-filter.awk
    trap "rm -f ${POM_FILTER}" EXIT

    # some local variables for the awk script
    local MV=modelVersion NM=name VS=version

    cat >${POM_FILTER} <<FILTER_END
{
    FS = "<|>";

    if (\$2 == "${MV}") { ${MV} = 1; ${NM} = ""; ${VS} = ""; next; }
    if (${MV} == 1 && \$2 == "${NM}") { ${NM} = \$3; next; }
    if (${MV} == 1 && \$2 == "${VS}") { ${VS} = \$3; next; }
    if (${NM} != "" && ${VS} != "") { print ${NM} " " ${VS}; exit; }
}
FILTER_END

    # Do the extraction
    awk -f ${POM_FILTER} ${POM}
}

GROUP_ID=$(groupId)
[ -z "${GROUP_ID}" ] && die "Group ID could not be identified."

PROJECT=$(project)
[ -z "${PROJECT}" ] && die "Project could not be identified."

COMMIT=$(git rev-parse HEAD)
[ -z "${COMMIT}" ] && die "Commit could not be identified."

GROUP_DIR=$(echo ${GROUP_ID} | sed -e 's/\./\//g')

M2="${HOME}/.m2"
REPO=repository
LOCAL_REPO="${M2}/${REPO}"
CATALOG_FILE=archetype-catalog.xml

STAGING=staging
STAGING_REPO=${STAGING}/${REPO}

function init {
    [ -d ${STAGING} ] && [ ! -d ${STAGING}/.git ] && die "${STAGING} is not a Git repository."
    [ -d ${STAGING} ] || git clone git@github.com:aqueance/maven.git ${STAGING}
}

function artifacts {
    echo "Deploying artifacts to ${STAGING}"

    # We are NOT using the deploy goal as it adds timestamp prefixes and such (Reproducible snapshot builds? Haha, good one.)
    #mvn deploy -Ddistribution "-DaltDeploymentRepository=staging::default::file://$(pwd)/${STAGING_REPO}"

    # Install the artifacts with checksum
    mvn -q clean install -Ddistribution -DcreateChecksum=true

    [ -d ""${LOCAL_REPO}/${GROUP_DIR}"" ] || die "Artifacts not found: ${LOCAL_REPO}/${GROUP_DIR}"

    # Copy the artifacts to the staging area
    (cd "${M2}"; tar cJf - "${REPO}/${GROUP_DIR}") | (cd ${STAGING}; tar xJf -)
}

function archetypes {
    [ -r "${M2}/${CATALOG_FILE}" ] || die "Archetype catalog not found: ${M2}/${CATALOG_FILE}."

    echo "Generating archetype catalog in ${STAGING}"

    # We are NOT using the archetype plugin as it fails to set the archetype description
    #mvn -N archetype:crawl -Drepository=${STAGING_REPO}

    # Use awk to parse the local archetype catalog and extract the archetypes by Fluid Tools
    local CATALOG_FILTER=catalog-filter.awk
    trap "rm -f ${CATALOG_FILTER}" EXIT

    # some local variables for the awk script
    local AT=archetype GP=groupId AF=artifactId VS=version DS=description AGP=${GROUP_ID}.archetypes

    cat >${CATALOG_FILTER} <<FILTER_END
{
    FS = "<|>";

    if (\$2 == "${AT}") { ${AT} = 1; I = \$1; II = I "  "; ${GP} = ""; AF = ""; ${VS} = ""; ${DS} = ""; next; }
    if (${AT} == 1 && \$2 == "${GP}") { ${GP} = \$3; next; }
    if (${AT} == 1 && \$2 == "${AF}") { ${AF} = \$3; next; }
    if (${AT} == 1 && \$2 == "${VS}") { ${VS} = \$3; next; }
    if (${AT} == 1 && \$2 == "${DS}") { ${DS} = \$3; next; }
    if (\$2 == "/${AT}") { ${AT} = 0; if (${GP} == "${AGP}") { print I "<${AT}>\n" II "<${GP}>" ${GP} "</${GP}>\n" II "<${AF}>" ${AF} "</${AF}>\n" II "<${VS}>" ${VS} "</${VS}>\n" II "<${DS}>" ${DS} "</${DS}>\n" I "</${AT}>"; } next; }

    print;
}
FILTER_END

    # Do the extraction
    awk -f ${CATALOG_FILTER} "${M2}/${CATALOG_FILE}" > "${STAGING}/${CATALOG_FILE}"
}

function upload {
    echo "Applying changes"

    (cd ${STAGING_REPO}; git add -A; git commit -m "${PROJECT} ${COMMIT} $(date)")
}

artifacts
archetypes
upload
