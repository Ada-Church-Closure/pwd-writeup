.intel_syntax   noprefix
.global _start

most_common_byte:
mov rbp, rsp
sub rsp, 512
xor rdx, rdx
count_words:
cmp rdx, rsi
je count_end
mov cl, [rdi + rdx]
mov bx, [rbp - 2 * rcx]
inc bx
mov [rbp - 2 * rcx], bx
inc rdx
jmp count_words
count_end:
mov rdx, 1
xor rcx, rcx # max_freq
xor rax, rax # max_freq_byte
find_max:
cmp rdx, 0x100
ja most_common_byte_over
cmp word ptr [rbp - 2 * rdx], cx
jle endcase
mov cx, [rbp - 2 * rdx]
mov al, dl
endcase:
inc rdx
jmp find_max
most_common_byte_over:
ret
_start:
call most_common_byte