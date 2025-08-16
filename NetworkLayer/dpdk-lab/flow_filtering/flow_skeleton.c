#include <stdint.h>

// rte的错误处理和flow规则
#include <rte_errno.h>
#include <rte_flow.h>

#include "common.h"

// 定了匹配的模式和动作(看源码)
#include "snippets/snippet_match_ipv4.h"

// 流规则的通用属性---优先级,方向
// postpone = 0 --->操作就是立即提交
struct rte_flow_attr flow_attr;
struct rte_flow_op_attr ops_attr = {.postpone = 0};   

// 非模板的创建flow的方式
static struct rte_flow *
create_flow_non_template(uint16_t port_id, struct rte_flow_attr *flow_attr,
                        struct rte_flow_item *patterns,
                        struct rte_flow_action *actions,
                        struct rte_flow_error *error)
{
    struct rte_flow *flow = NULL;

    if (rte_flow_validate(port_id, flow_attr, patterns, actions, error) == 0) {
        flow = rte_flow_create(port_id, flow_attr, patterns, actions, error);
    } else {
        fprintf(stderr, "rte_flow_validate failed: %s (%s)\n",
                error && error->message ? error->message : "no message",
                rte_strerror(rte_errno));
    }
    return flow;
}

// 用模板API规则进行创建
static struct rte_flow *
create_flow_template(uint16_t port_id, struct rte_flow_op_attr *ops_attr,
                    struct rte_flow_item *patterns,
                    struct rte_flow_action *actions,
                    struct rte_flow_error *error)
{
    // 更换头文件的话我们就会调用不同的函数,这里其实在头文件中是宏定义,为了统一.
    struct rte_flow_template_table *table = 
        snippet_skeleton_flow_create_table(port_id, error);
    if (table == NULL) {
        printf("Failed to create table: %s (%s)\n",
               error->message, rte_strerror(rte_errno));
        return NULL;
    }

    return rte_flow_async_create(port_id,
            1,                     // 流队列 ID（用于异步操作）
            ops_attr,
            table,                 // 预定义的模板表
            patterns,
            0,                     // 模式模板索引（0 表示第一个模板）
            actions,
            0,                     // 动作模板索引（0 表示第一个模板）
            0,                     // 用户数据（可忽略）
            error);
}

// 流规则生成的入口
struct rte_flow *
generate_flow_skeleton(uint16_t port_id, struct rte_flow_error *error, int use_template_api)
{
    struct rte_flow_action actions[MAX_ACTION_NUM] = {0};
    struct rte_flow_item patterns[MAX_PATTERN_NUM] = {0};

    // 从代码片段加载默认动作和模式
    // 这里默认填充的就是基础的规则
    snippet_skeleton_flow_create_actions(actions);
    snippet_skeleton_flow_create_patterns(patterns);

    // 根据 API 类型选择创建方式
    if (use_template_api)
        return create_flow_template(port_id, &ops_attr, patterns, actions, error);
    else
        return create_flow_non_template(port_id, &flow_attr, patterns, actions, error);
}