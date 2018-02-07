#!/bin/bash

############################################################
#
# This script restores the quince database from the backup
# transferred from the production server every night.
# If no database-bckup file is found, it will try to fetch
# the latest backup file from poseidon, and restore from
# this.
#
# The script also updates the local file-store from the
# production server.
#
############################################################

# Get db username, password etc
username=$(./scripts/get_setup_property.sh username)
if [ -z $username ]
then
  exit 1
fi
password=$(./scripts/get_setup_property.sh password)
database=$(./scripts/get_setup_property.sh database)

verbose=0

while getopts "v" opt; do
  case "$opt" in
  v)  verbose=1
    ;;
  esac
done

# DB backup file on test server opdated daily from prod
file=/data/shared/quince_backups/quince.sql.gz

if [ ! -f $file ];
then
  # If not on the test server, try fetchind db backup directly from prodserver
  tmpfile=$(mktemp /tmp/db_restore_from_prod.sh.XXXX)
  remotefile=poseidon.uib.no:/var/backup/shared/backups/quince.sql.gz

  if [ $verbose -eq 1 ]
  then
    echo "Fetching database backup from $remotefile"
  fi

  scp $remotefile $tmpfile

  if [ -s $tmpfile ]
  then
    file=$tmpfile
  else
    echo "No database backup found at $file. Also, fetching file from "\
         "$remotefile failed. Aborting!" >&2
    exit 1
  fi
fi

if [ $verbose -eq 1 ]
then
  echo "Restore database $database with user $username, pw $password"
fi

# Restore database
echo "drop database $database"|mysql -u$username -p$password
echo "create database $database character set utf8" |mysql -u$username -p$password
zcat $file | \
  mysql -u$username -p$password $database --default-character-set utf8

# Delete tempfile, don't display errors
rm $tmpfile 2>/dev/null

filestore=$(./scripts/get_setup_property.sh filestore)

# Filestore path on test server opdated daily from prod
file=/data/shared/quince_backups/QUINCE_FILE_STORE/QUINCE_FILE_STORE/.

if [ ! -d $file ]
then
  # Fetch filestore directly from poseidon
  file=poseidon.uib.no:/var/backup/quince/QUINCE_FILE_STORE/.
fi

# Just make sure the filestore is not pointing directly to the folder files are
# copied to.
if [ ! $filestore -ef $file ]
then
  options=-rq
  if [ $verbose -eq 1 ]
  then
    options=-rv
  fi
  # copy the filestore
  rsync $options $file $filestore
fi