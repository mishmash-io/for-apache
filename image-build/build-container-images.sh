#!/bin/bash
#
#    Copyright 2026 Mishmash IO UK Ltd.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

stages="stacks-jackson stacks-gson stacks-yaml stacks-jackson-yaml \
    stacks-logging stacks-slf4j stacks-bouncy-castle stacks-netty \
    stacks-sasl-oidc stacks-opentelemetry-agent stacks-jetty-base \
    stacks-jetty-client stacks-jetty-server stacks-jetty-servlet \
    stacks-quorum-client stacks-quorum-server quorum-server \
    stacks-quorum-server-controller stacks-quorum-server-admin-rest \
    "

function get_version() {
    local artifact=${2:-io.mishmash.stacks:distributed-computing-stacks}
    local v=$(mvn --offline org.apache.maven.plugins:maven-help-plugin:3.5.2:evaluate -Dexpression=$1 -Dartifact=$artifact -q -DforceStdout 2> /dev/null)
    echo "$v"
}

mvn dependency:get -Dartifact=org.apache.maven.plugins:maven-help-plugin:3.5.2 &> /dev/null

echo "Collecting version information..."

jackson_version=$(get_version "jackson.version")
logging_version=$(get_version "log4j2.version")
jetty_version=$(get_version "jetty.version")
quorum_version=$(get_version "zookeeper.stable.version")-mmio.$(get_version "zookeeper.stable.mishmash.io.patch").$(get_version "zookeeper.stable.mishmash.io.update")

declare -A versions

versions["stacks-jackson"]=$jackson_version
versions["stacks-gson"]=$(get_version "gson.version")
versions["stacks-yaml"]=$(get_version "snakeyaml.version")
versions["stacks-jackson-yaml"]=$jackson_version
versions["stacks-logging"]=$logging_version
versions["stacks-slf4j"]=$(get_version "slf4j.version")
versions["stacks-bouncy-castle"]=$(get_version "bouncycastle.version")
versions["stacks-netty"]=$(get_version "netty.version")
versions["stacks-sasl-oidc"]=$(get_version "misc-openid.version")
versions["stacks-opentelemetry-agent"]=$(get_version "opentelemetry.agent.version")
versions["stacks-jetty-base"]=$jetty_version
versions["stacks-jetty-client"]=$jetty_version
versions["stacks-jetty-server"]=$jetty_version
versions["stacks-jetty-servlet"]=$jetty_version
versions["stacks-quorum-client"]=$quorum_version
versions["stacks-quorum-server"]=$quorum_version
versions["quorum-server"]=$quorum_version
versions["stacks-quorum-server-controller"]=$quorum_version
versions["stacks-quorum-server-admin-rest"]=$quorum_version

echo "Done! Using versions:"
for key in $stages; do
    echo "$key: ${versions[$key]}"
done

for stage in $stages
do
    echo "Building stage: $stage:${versions[$stage]}"
    podman build --target "$stage" \
        --tag "docker.io/mishmashio/$stage:latest" \
        --tag "docker.io/mishmashio/$stage:${versions[$stage]}" \
        -v=$HOME/.m2:/root/.m2:rw,Z .
done
