#!/usr/bin/env bash
version=$(sed '/^\#/d' gradle.properties | grep 'VERSION_NAME' | tail -n 1 | cut -d "=" -f2-)

# This user is just a placeholder - no public commit should be attributed to them
git config user.name "DeltaDNA"
git config user.email "placeholder@example.com"

git tag $version

git push origin $version

git checkout master
git merge origin/master

git remote rm github
git remote add github git@github.com:deltaDNA/android-sdk.git

git push -f github HEAD
git push github $version

