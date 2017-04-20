'use strict';
/*
 * Build file for JS and CSS assets.
 *
 *     npm install
 *     node_modules/.bin/gulp assets
 */
const gulp = require('gulp');
const fs = require('fs');
const gutil = require('gulp-util');
const rename = require('gulp-rename');
const sourcemaps = require('gulp-sourcemaps');
const uglify = require('gulp-uglify');
const concat = require('gulp-concat');
const postcss = require('gulp-postcss');
const autoprefix = require('autoprefixer');
const cleanCSS = require('gulp-clean-css');
const less = require('gulp-less');
const clean = require('gulp-clean');
const hashsum = require('./gulp/hashsum');

/* Recommended gulpopts.json:

 {
  "env": {
   "PRODUCTION": false
  }
 }

 */
let gulpOpts = { env: {} };
try {
  fs.accessSync('./gulpopts.json', fs.R_OK);
  gulpOpts = require('./gulpopts.json');
  gutil.log('Got opts');
} catch (e) {
  gutil.log(gutil.colors.yellow('No gulpopts.json ('+e.message+')'));
}
function option(name, fallback) {
  const value = process.env[name] || gulpOpts.env[name];
  if (value === undefined) return fallback;
  return (value === 'true' || value === true);
}

const uglifyOptions = { output: { ascii_only:true } };

// Copy static to target
gulp.task('copy-assets', () => {
  return gulp.src('src/main/assets/**/*')
    .pipe(gulp.dest('build/rootContent'));
});

// Copy id7 to render/id7
gulp.task('copy-id7', ['copy-assets'], () => {
  const base = 'node_modules/id7/dist';

  return gulp.src(`${base}/**/*`, { base: base })
    .pipe(gulp.dest('build/rootContent/static/id7'));
});

// Concat to create various script files
function concatScripts(name, target, srcs, minify, dependencies) {
  gulp.task(name, ['copy-assets'].concat(dependencies || []), () => {
    const base = 'build/rootContent/static';

    return gulp.src(srcs.map(s => `${base}/${s}`), {base: base})
      .pipe(sourcemaps.init())
      .pipe(concat(target))
      .pipe(minify ? uglify(uglifyOptions) : gutil.noop())
      .pipe(sourcemaps.write({ includeContent: false, sourceRoot: '/static' }))
      .pipe(gulp.dest(base));
  });
}

concatScripts('concat-renderjs', 'js/render.js', [
  'libs/jquery/jquery-1.8.3.js',
  'js/id6scripts.js',
  'libs/jquery-ui/js/jquery-ui-1.9.2.custom.js',
  'libs/jquery.delayedObserver.js',
  'libs/jquery-caret/jquery.caret.1.02.js',
  'libs/jquery-fixedheadertable/jquery.fixedheadertable.min.js',
  'libs/jquery-rating/jquery.rating.pack.js',
  'libs/jquery-tablesorter/jquery.tablesorter.min.js',
  'js/moment.js',
  'js/moment-timezone-london.js',
  'libs/popup/popup.js',
  'libs/bootstrap/js/bootstrap.js',
  'libs/bootstrap-editable/js/bootstrap-editable.js',
  'libs/bootstrap-datetimepicker/js/bootstrap-datetimepicker.js',
  'libs/spin-js/spin.min.js',
  'libs/spin-js/jquery.spin.js',
  'js/modernizr.js',
  'js/browser-info.js',
  'js/activity-streams.js',
], true);
concatScripts('concat-commonjs', 'js/common.js', [
  'js/scripts.js',
  'js/jquery-copyable.js',
  'js/jquery-biglist.js',
  'js/jquery-confirmmodal.js',
  'js/jquery-details.js',
  'js/jquery-collapsible.js',
  'js/jquery-tableform.js',
  'js/jquery-draganddrop.js',
  'js/jquery-expandingTable.js',
  'js/jquery.form.js',
  'js/jquery-filteredlist.js',
  'js/jquery-radiocontrolled.js',
  'js/jquery-scrolltofixed.js',
  'js/jquery-fixheaderfooter.js',
  'js/jquery.are-you-sure.js',
  'js/userpicker.js',
  'js/flexipicker.js',
  'js/ajax-popup.js',
  'js/select-deselect-checkboxes.js',
  'js/combo-typeahead.js',
  'js/map-popups.js',
], true);
concatScripts('concat-coursesjs', 'js/courses.js', [
  'js/common.js',
  'js/coursework-admin.js',
  'js/coursework-submission.js',
], true, ['concat-commonjs']);
concatScripts('concat-examsjs', 'js/exams.js', [
  'js/common.js',
  'js/exams-admin.js',
], true, ['concat-commonjs']);
concatScripts('concat-homejs', 'js/home.js', [
  'js/common.js',
], true, ['concat-commonjs']);
concatScripts('concat-schedulingjs', 'js/scheduling.js', [
  'js/common.js',
], true, ['concat-commonjs']);
concatScripts('concat-profilesjs', 'js/profiles.js', [
  'js/common.js',
  'js/profiles-render.js',
  'js/fullcalendar.js',
  'js/allocate-associations.js',
], true, ['concat-commonjs']);
concatScripts('concat-groupsjs', 'js/groups.js', [
  'js/common.js',
  'js/groups-admin.js',
  'js/groups-render.js',
  'js/attendance-recording.js',
], true, ['concat-commonjs']);
concatScripts('concat-adminjs', 'js/admin.js', [
  'js/common.js',
], true, ['concat-commonjs']);
concatScripts('concat-attendancejs', 'js/attendance.js', [
  'js/common.js',
  'js/attendance-render.js',
  'js/attendance-recording.js',
], true, ['concat-commonjs']);
concatScripts('concat-reportsjs', 'js/reports.js', [
  'js/common.js',
], true, ['concat-commonjs']);

// Use gulp.parallel when we upgrade to Gulp 4
gulp.task('concat-scripts-id6', [
  'concat-renderjs',
  'concat-commonjs',
  'concat-coursesjs',
  'concat-examsjs',
  'concat-homejs',
  'concat-schedulingjs',
  'concat-profilesjs',
  'concat-groupsjs',
  'concat-adminjs',
  'concat-attendancejs',
  'concat-reportsjs',
]);

concatScripts('concat-renderjs-id7', 'js/id7/render.js', [
  'id7/js/id7-bundle.js',
  'libs/jquery-ui-interactions/jquery-ui-1.11.4-custom.js',
  'libs/jquery.delayedObserver.js',
  'libs/jquery-caret/jquery.caret-1.5.2.js',
  'libs/jquery-fixedheadertable/jquery.fixedheadertable.min.js',
  'libs/jquery-tablesorter/jquery.tablesorter.min.js',
  'js/moment.js',
  'js/moment-timezone-london.js',
  'libs/popup/popup.js',
  'libs/bootstrap-datetimepicker/js/bootstrap-datetimepicker.js',
  'libs/bootstrap3-typeahead/bootstrap3-typeahead.js',
  'libs/bootstrap3-typeahead/no-conflict.js',
  'libs/bootstrap3-editable/js/bootstrap-editable.js',
  'libs/spin-js/spin.min.js',
  'libs/spin-js/jquery.spin.js',
  'js/jquery-copyable.js',
  'js/jquery-details.js',
  'js/jquery.are-you-sure.js',
  'js/id7/jquery-draganddrop.js',
  'js/jquery-filteredlist.js',
  'js/jquery-collapsible.js',
  'js/id7/jquery-fixheaderfooter.js',
  'js/jquery-scrolltofixed.js',
  'js/jquery-radiocontrolled.js',
  'js/userpicker.js',
  'js/id7/flexipicker.js',
  'js/ajax-popup.js',
  'js/browser-info.js',
  'js/combo-typeahead.js',
  'js/id7/map-popups.js',
  'js/jquery-biglist.js',
  'js/id7/jquery-expandingTable.js',
  'js/jquery.form.js',
  'js/id7/jquery-tableform.js',
  'js/select-deselect-checkboxes.js',
], true, ['copy-id7']);
concatScripts('concat-commonjs-id7', 'js/id7/common.js', [
  'js/id7/scripts.js',
], true);
concatScripts('concat-homejs-id7', 'js/id7/home.js', [
  'js/id7/common.js',
  'js/activity-streams.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-reportsjs-id7', 'js/id7/reports.js', [
  'js/id7/common.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-adminjs-id7', 'js/id7/admin.js', [
  'js/id7/common.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-groupsjs-id7', 'js/id7/groups.js', [
  'js/id7/common.js',
  'js/groups-admin.js',
  'js/groups-render.js',
  'js/id7/attendance-recording.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-examsjs-id7', 'js/id7/exams.js', [
  'js/id7/common.js',
  'js/jquery-confirmmodal.js',
  'js/exams-admin.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-profilesjs-id7', 'js/id7/profiles.js', [
  'js/id7/common.js',
  'js/profiles-render.js',
  'js/fullcalendar.js',
  'js/id7/allocate-associations.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-cm2js-id7', 'js/id7/cm2.js', [
  'js/id7/common.js',
  'js/cm2-admin.js',
  'js/filters.js',
  'js/fullcalendar.js',
  'js/id7/textList.js',
], true, ['concat-commonjs-id7']);
concatScripts('concat-attendancejs-id7', 'js/id7/attendance.js', [
  'js/id7/common.js',
  'js/id7/attendance-recording.js',
  'js/id7/attendance-render.js',
], true, ['concat-commonjs-id7']);

// Use gulp.parallel when we upgrade to Gulp 4
gulp.task('concat-scripts-id7', [
  'concat-renderjs-id7',
  'concat-commonjs-id7',
  'concat-homejs-id7',
  'concat-reportsjs-id7',
  'concat-adminjs-id7',
  'concat-groupsjs-id7',
  'concat-examsjs-id7',
  'concat-profilesjs-id7',
  'concat-cm2js-id7',
  'concat-attendancejs-id7',
]);

gulp.task('concat-scripts', ['concat-scripts-id6', 'concat-scripts-id7']);

// Compile any less files
gulp.task('compile-less', ['copy-assets', 'copy-id7'], () => {
  const base = 'build/rootContent/static';

  const srcs = [
    `${base}/**/*.less`,
    `!${base}/**/*.inc.less`,
    `!${base}/**/bootstrap/less/**`,
    `!${base}/**/bootstrap/*.less`,
    `!${base}/**/font-awesome/less/**`,
    `!${base}/id7/**`,
  ];

  return gulp.src(srcs, {base: base})
    .pipe(sourcemaps.init())
    .pipe(less({
      // Allow requiring less relative to node_modules, plus any other dir under node_modules
      // that's in styleModules.
      paths: ['node_modules', 'node_modules/id7/less']
    }))
    .pipe(postcss([
      autoprefix({ browsers: ['> 1% in GB', 'last 2 versions', 'IE 9'] }),
    ]))
    .pipe(sourcemaps.write({ includeContent: false, sourceRoot: '/static' }))
    .pipe(gulp.dest(base));

});

gulp.task('all-assets', ['copy-assets', 'copy-id7', 'concat-scripts', 'compile-less']);

// Batch hash to WEB-INF/static-hashes.properties
gulp.task('hash-assets', ['all-assets'], () => {
  const base = 'build/rootContent/static';

  return gulp.src(`${base}/**/*`, { base: base })
    .pipe(hashsum({
      base: base,
      filename: 'static-hashes.properties',
      dest: 'build/rootContent/WEB-INF',
      maxLength: 12,
      salt: 'a',
    }));
});

gulp.task('clean', () => {
  return gulp.src('build/rootContent/static', { read: false })
    .pipe(clean());
});

// Shortcuts for building all asset types at once
gulp.task('assets', ['all-assets', 'hash-assets']);
gulp.task('watch-assets', () => {
  return gulp.watch('src/main/assets/**/*', ['assets']);
});
gulp.task('default', ['assets', 'watch-assets']);