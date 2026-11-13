from pathlib import Path
import subprocess
p=Path('app/src/main/java/com/vahta/app/Organizer.kt')
commit='710245d448f4'
original=subprocess.check_output(['git','show',f'{commit}:{p.as_posix()}'],text=True)
old='''                                item.title,
                                modifier = Modifier.weight(1f).clickable {'''
new='''                                (if (activeCategory == "Подарки") item.unit.ifBlank { item.title } else item.title),
                                modifier = Modifier.weight(1f).clickable {'''
old2='''                        } else if (item.unit.isNotBlank()) Text(item.unit, color = orgMuted, fontSize = 13.sp)'''
new2='''                        } else if (activeCategory == "Подарки") {
                            Text(item.title, color = orgMuted, fontSize = 13.sp)
                        } else if (item.unit.isNotBlank()) Text(item.unit, color = orgMuted, fontSize = 13.sp)'''
assert original.count(old)==1, 'Cannot locate gift card title'
assert original.count(old2)==1, 'Cannot locate gift card subtitle'
updated=original.replace(old,new).replace(old2,new2)
p.parent.mkdir(parents=True,exist_ok=True)
p.write_text(updated)
print('Restored modern Organizer.kt and updated only gifts card')
