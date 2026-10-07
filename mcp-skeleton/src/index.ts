// 문항 은행 MCP 서버 골격 (TypeScript SDK v2 · stdio)
//
// stdio 서버에서는 stdout 이 곧 프로토콜 채널이다.
// 로그는 console.error 만 쓴다(stdout 으로 찍는 로그 함수는 절대 쓰지 않는다).
import { McpServer } from '@modelcontextprotocol/server';
import { StdioServerTransport } from '@modelcontextprotocol/server/stdio';
import * as z from 'zod/v4';
import { searchItems } from './itemApi.js';

const server = new McpServer({ name: 'item-bank', version: '1.0.0' });

// 예시 도구: 서버가 살아 있는지 확인한다. 입력 없음.
server.registerTool(
  'ping',
  {
    description: '서버 연결 확인용 예시 도구. "pong" 을 돌려준다.'
  },
  async () => ({
    content: [{ type: 'text', text: 'pong' }]
  })
);

server.registerTool(
  'search_items',
  {
    description: '문항 내용이나 태그를 확인해야 할 때 키워드 · 단원 · 난이도로 문항 은행을 검색한다.',
    inputSchema: z.object({
      keyword: z.string().describe('문항 본문 또는 태그에서 검색할 키워드'),
      unit: z.string().optional().describe('단원 코드 또는 단원 이름 일부'),
      difficulty: z.enum(['하', '중', '상']).optional().describe('난이도: 하 · 중 · 상'),
      limit: z.number().int().min(1).max(20).default(5).describe('최대 검색 건수: 1~20, 기본 5')
    })
  },
  async ({ keyword, unit, difficulty, limit }) => {
    const items = await searchItems({ keyword, unit, difficulty, limit });
    const text = items.length > 0
      ? JSON.stringify(items, null, 2)
      : `검색 결과 없음: ${JSON.stringify({ keyword, unit, difficulty, limit })}`;
    return { content: [{ type: 'text', text }] };
  }
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('item-bank MCP server running on stdio');
}

main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
