#!/bin/bash
set -e

# Needs to be compiled in common module first
cd /Users/liuxingyu/NexusERP-X/
mvn clean install -pl nexus-common -am
