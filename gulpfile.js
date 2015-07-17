//some ideas borrowed from danharper's gist on github
var gulp = require('gulp');
var browserify = require('browserify');
var babelify = require('babelify');
var source = require('vinyl-source-stream');
var child_process = require('child_process');

gulp.task('buildClient', function(){
    browserify({
      entries: ['client/Client.js'],
      debug: true
    })
  .transform(babelify)
  .bundle()
  .pipe(source('app.js'))
  .pipe(gulp.dest('./client/build'))
});

gulp.task('serve', function(){
  var server = child_process.spawn('nodemon', ['--exec', 'babel-node', '--', 'Server.js'], {cwd: './server'});
  server.stdout.on('data', function(data){
    console.log(data.toString());
  });
  server.stderr.on('data', function(data){
    console.warn(data.toString());
  });
});

gulp.task('watch', function(){
  var watchFiles = ['client/index.html', 'client.css', 'client/utils/*.js', 'client/stores/*.js',
    'client/actions/*.js', 'client/dispatcher/*.js', 'client/components/*.js', 'client/constants/*.js'];
  gulp.watch(watchFiles, ['buildClient']);
});

gulp.task('copyTriggerfishAPIFile', function(){
  child_process.spawn('npm', ['run', 'copy-triggerfishAPI']);
});

gulp.task('default', ['copyTriggerfishAPIFile', 'watch', 'buildClient', 'serve']);
