#!/usr/bin/env bash
version=$(sed '/^\#/d' gradle.properties | grep 'VERSION_NAME' | tail -n 1 | cut -d "=" -f2-)

git tag $version

git push origin $version

git checkout master
git merge origin/master

git remote rm github
git remote add github git@github.com:deltaDNA/android-sdk.git

git push -f github HEAD
git push github $version

./scripts/publish-javadocs
