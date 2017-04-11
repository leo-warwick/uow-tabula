// Based on https://github.com/remko/gulp-hashsum/blob/master/index.js
'use strict';

const crypto = require('crypto');
const gutil = require('gulp-util');
const _ = require('lodash');
const mkdirp = require('mkdirp');
const slash = require('slash');
const through = require('through');

const fs = require('fs');
const path = require('path');

module.exports = function hashsum(options) {
  options = _.defaults(options || {}, {
    filename: 'static-hashes.properties',
    dest: process.cwd(),
    hash: 'md5',
    force: false,
    delimiter: '=',
    maxLength: 12,
    salt: 'a',
  });

  const hashesFilePath = path.resolve(options.dest, options.filename);
  let hashes = {};

  const base = options.base || path.dirname(hashesFilePath);

  function processFile(file) {
    if (file.isNull()) {
      return;
    }

    if (file.isStream()) {
      this.emit('error', new gutil.PluginError('gulp-hashsum', 'Streams not supported'));
      return;
    }

    const filePath = path.resolve(options.base || options.dest, file.path);
    let hash =
      crypto.createHash(options.hash)
        .update(file.contents, 'binary')
        .update(options.salt)
        .digest('hex');

    hash = parseInt(hash, 16);
    if (hash < 0) hash = -hash;

    hash = hash.toPrecision(21).replace(/[^\d]/g, '');
    if (options.maxLength && hash.length > options.maxLength) {
      hash = hash.substring(0, options.maxLength);
    }

    hashes[slash(path.relative(base, filePath))] = hash;

    this.push(file);
  }

  function writeSums() {
    const lines = _.keys(hashes).sort().map(function (key) {
      return key + options.delimiter + hashes[key] + '\n';
    });
    const contents = lines.join('');

    const data = new Buffer(contents);

    if (options.force || !fs.existsSync(hashesFilePath) || Buffer.compare(fs.readFileSync(hashesFilePath), data) !== 0) {
      mkdirp.sync(path.dirname(hashesFilePath));
      fs.writeFileSync(hashesFilePath, data);
    }
    this.emit('end');
  }

  return through(processFile, writeSums);
}