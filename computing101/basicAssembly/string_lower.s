.intel_syntax noprefix
.global _start

str_lower:
xor rdx, rdx
cmp rdi, 0x0
je str_lower_end
loop_1:
cmp [rdi], 0x00
je str_lower_end
cmp [rdi], 0x5a
ja endcase
call_foo:
push rdi
mov rax, 0x403000
mov rdi, [rdi]
call rax
pop rdi
mov [rdi], rax
inc rdx
endcase:
inc rdi
jmp loop_1
str_lower_end:
mov rax, rdx
ret

_start:
call str_lower