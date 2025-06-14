.intel_syntax noprefix
.global _start
_start:
xor rax, rax
cmp rdi, 0
je endcase
loop:
mov bl, [rdi + rax]
cmp bl, 0
je endcase
inc rax
jmp loop
endcase: