" Vim syntax file
" Language:     Mar
" Maintainer:   Francisco Sant'Anna <francisco.santanna@gmail.com>
" Last Change:  2024 October

if exists("b:current_syntax")
    finish
endif

syn match   Comment   ";;.*$"
syn region  Comment   start=/;;;$/ end=/;;;$/

syn region  String    start=/\v"/ skip=/\v(\\[\\"]){-1}/ end=/\v"/
syn match   String    "'.'"
"syntax region String start=/\v'/ skip=/\v(\\[\\"]){-1}/ end=/\v'/

syn match   Constant  '\d\+'
syn keyword Constant  false null true

syn keyword Function  expect print

syn match   Type      '[A-Z][a-zA-Z0-9]\+'

syn match   Statement '[\+\-\*\/\%\>\<\=\|\&\~]'

syn keyword Statement await break do catch compile coro create data defer else
syn keyword Statement emit exec escape false func if in include loop match null
syn keyword Statement par par_and par_or
syn keyword Statement print resume return set spawn start task test throw true
syn keyword Statement until var yield with where while

syn keyword Todo      TODO FIXME XXX
syn region  String    start='"' end='"'
