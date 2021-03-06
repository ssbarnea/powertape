#!/usr/bin/env groovy
/*
   Shared libraries cannot be tested with CR/PR, as Jenkins can only load
   a library from a defined branch.

   Due to this, we will merge to @master only from green branches.
*/
@Library('powertape') _

properties([
        // https://st-g.de/2016/12/parametrized-jenkins-pipelines
        parameters([
                booleanParam(
                  name: 'DEBUG',
                  defaultValue: true,
                  description: 'Enables debug mode which increase verbosity level.'),
                string(
                  name: 'GERRIT_REFSPEC',
                  defaultValue: '+refs/heads/master:refs/remotes/origin/master',
                  description: 'The REFSPEC of the component'),
                string(
                  name: 'GERRIT_BRANCH',
                  defaultValue: 'master',
                  description: 'The branch of the component')

        ]),

        // schedule job to run daily between 1am-2am
        pipelineTriggers([
                // [$class: 'GitHubPushTrigger'], // enable only if plugin is installed
                pollSCM('H/5 * * * *'),
                // cron('H 01 * * *')
        ])
])


// code that needs behaviour testing outside the pipeWrapper()

// if ansicolor is not enabled these should be without color
log "debug outside ansiColor block", level: "DEBUG"
log "info outside ansiColor block", level: "INFO"
log "warn outside ansiColor block", level: "WARN"
log "error outside ansiColor block", level: "ERROR"
log "fatal outside ansiColor block", level: "FATAL"

println "INFO: ${ false } == ${False} and ${ true } == ${ True }"
println "INFO: ${ false  == False } vs ${ true == True }"

if (gconfig('hello') != 'world!' || gconfig('foo', 'x') != 'x' || gconfig('bar') != null) {
   println "ERROR: gconfig test failed."
   currentBuild.result = 'FAILURE'
   }

// tests the mega-wrapper that hides most used common functionality
pipeWrapper(email: false, gerritReport: true) {
  log "debug inside ansiColor block", level: "DEBUG"
  log "info inside ansiColor block", level: "INFO"
  log "warn inside ansiColor block", level: "WARN"
  log "error inside ansiColor block", level: "ERROR"
  log "fatal inside ansiColor block", level: "FATAL"

  // some tools could fail if no TERM is defined
  // env.TERM = env.TERM ?: 'xterm-color'

  // Inspired from http://unix.stackexchange.com/questions/148/colorizing-your-terminal-and-shell-environment
  env.ANSIBLE_FORCE_COLOR = env.ANSIBLE_FORCE_COLOR ?: 'true'
  env.CLICOLOR = env.CLICOLOR ?: '1'
  env.LSCOLORS = env.LSCOLORS ?: 'ExFxCxDxBxegedabagacad'
  def error = null

  // testing that we can get global settings using gconfig()
  println "hello=${gconfig('hello')} wrong_key=${gconfig('wron_key', 'default')}"

  node('master') {

      try {

          // we don't want any leftovers to influence our execution (like previous logs)
          step([$class: 'WsCleanup'])

          checkout scm

          // start-of-unittests
          stage('gitClean') {
              gitClean()
          }

          sh "set > .envrc"

          stage("mkdtemp") {
             def x = mkdtemp('cd-')
             println "${x}"
          }

          stage("md5") {
             def jobname = "${env.JOB_NAME}#${env.BUILD_NUMBER}"
             def x = md5(jobname, 6)
             println "md5('${jobname}', 6) => ${x}"
          }

          stage("sh2") {
              withEnv(["MAX_LINES=2"]) {
                  // should display 1,2,4,5 (missing 3) and sh.log
                  log "[sh2] 001"
                  def result = sh2 script: "pwd; seq 5; exit 0", returnStatus: true

                  if (result != 0) {
                      println "ERROR: Got [${result}] status code instead of expected [0]"
                      currentBuild.result = 'FAILURE'
                  }

                  log "[sh2] 002"
                  sh "mkdir -p foo"
                  dir("foo") {
                    // should generate and archive $WORKSPACE/.sh/ansitest.log
                    // even if the current directory is $WORKSKAPCE/foo
                    withEnv(['STAGE_NAME=foo']) {
                        sh2 "echo 'foo!'; seq 10"
                        sh2 script: "echo 'foo2!'; seq 10", compress: true
                        }
                  }

                  log "[sh2] 003"
                  // should create "date.log"
                  sh2 script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"',
                      basename: "date"

                  log "[sh2] 004"
                  // should generate sh-1.log.gz
                  sh2 script: """#!/bin/bash
                      for i in \$(seq 100)
                      do
                        printf "\$i\n"
                        sleep 0.5
                      done""",
                      compress: true,
                      progressSeconds: 5

                  log "[sh2] 005"
                  // this should not generate a log file or limit the output due
                  // to returnStdout: true
                  result = sh2 script: "seq 5", returnStdout: true
                  println "result = [${result}]"
                  // normalize output
                  result = result.replaceAll('\\r\\n?', '\n').trim()
                  if (result != '1\n2\n3\n4\n5') {
                      println "FAILURE: Unexpected result: [$result]"
                      currentBuild.result = 'FAILURE'
                  }

                  log "[sh2] 006"
                  sh2 basename: "sh-006", script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"'

                  log "[sh2] testing escaping of complex commands"
                  cmd = "true && printf \"${getANSI('PASS')}PASS${getANSI()}\n\""
                  sh2 basename: "sh-007", echoScript: true, cmd

                  log "[niceprefix] ${niceprefix()}"

                  log "[notifyBuild]"

                  def templates = ["groovy-html.template", "groovy-text.template",
                                   "groovy-html-larry.template", "html-with-health-and-console.jelly",
                                   "html.jelly", "html_gmail.jelly",
                                   "static-analysis.jelly", "text.jelly"]
                  for(int i=0;i<templates.size();i++) {
                      notifyBuild(email:true,
                                  template: templates[i],
                                  msg: "Generated using ${templates[i]} template.",
                                  dry: false)
                  }

              } // withEnv
          } // sh2
      } // end-try
      catch (ex) {
          println "Exception ${ex.getClass()} received: ${ex}"
          error = ex
          currentBuild.result = 'FAILURE'
          throw ex
      }
      finally {
          stage('archive') {

              archiveArtifacts artifacts: '.envrc',
                               fingerprint: false,
                               allowEmptyArchive: true
              // defaultExcludes: true
              // caseSensitive: false
              // onlyIfSuccessful: false
          } // end-clean
          println getBuildInfo(currentBuild)
          println getBuildMessage()
      } // finally
  } // node

} // pipeWrapper
