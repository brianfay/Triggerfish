//some ideas borrowed from danharper's gist on github
var gulp = require('gulp');
var browserify = require('browserify');
var babelify = require('babelify');
var source = require('vinyl-source-stream');
var child_process = require('child_process');

gulp.task('buildClient', function(){
    browserify({
      entries: ['client/Client.js', 'client/Components.js', 'client/Utils.js'],
      debug: true
    })
  // )
  .transform(babelify.configure({
     stage: 0
  }))
  .bundle()
  .pipe(source('app.js'))
  .pipe(gulp.dest('./client/build'));
});

gulp.task('serve', function(){
  var server = child_process.spawn('nodemon', ['--harmony', 'server/Server.js']);
  server.stdout.on('data', function(data){
    console.log(data.toString());
  });
  server.stderr.on('data', function(data){
    console.warn(data.toString());
  });
});
  

gulp.task('default', ['watch', 'buildClient', 'serve']);
gulp.task('watch', function(){
  gulp.watch('./client/*', ['buildClient']);
});
