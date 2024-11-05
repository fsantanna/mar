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

syn keyword Statement break do coro create data else
syn keyword Statement false func if loop null print resume
syn keyword Statement return set start true var exec
syn keyword Statement yield

syn keyword Todo      TODO FIXME XXX
syn region  String    start='"' end='"'
