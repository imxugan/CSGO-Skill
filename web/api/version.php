<?php

/**
 * THIS SHOULD BE STATIC.
 * Dynamic sounds cool, but if we need to test live versions, we need to change
 * the database version without affecting current users. When we are ready to
 * roll out a new version, we simply change this file and the new version is
 * officially released.
 *
 * The new version files should already be in the Database and on a timed release
 * in the app store if needed. We just wait for the app to update on the store
 * and publish the new `version.php` and boom, super smooth version change.
 */

$MAJOR = 0;
$MINOR = 8;
$POINT = 0;

echo "v" + $MAJOR + "." + $MINOR + "." + $POINT;

?>
