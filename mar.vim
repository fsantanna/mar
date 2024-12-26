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

syn keyword Function  print

syn match   Type      '[A-Z][a-zA-Z0-9]\+'

syn match   Statement '[\+\-\*\/\%\>\<\=\|\&\~]'

syn keyword Statement break do catch compile coro create data defer else exec
syn keyword Statement escape false func if in include loop match null print
syn keyword Statement resume return set start test throw true var yield

syn keyword Todo      TODO FIXME XXX
syn region  String    start='"' end='"'
