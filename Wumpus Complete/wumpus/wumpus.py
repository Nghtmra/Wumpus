import pygame
import random
import time
import sys
pygame.init()

#===============================================================================
#                       Functions Area                                         =
#===============================================================================

def check_neighbor_rooms(pos, item_list):
    """ Checks each orthagonal cell next to pos for the requested item
    returns True as soon as the item is found.
    """
    exits = cave[pos]
    return any(item in cave[pos] for item in item_list)
        
def draw_room(pos, screen):
    """ Draws the room in the back buffer
    """
    x=0
    y=1
    exits = cave[player_pos]
    screen.fill((0,0,0))

    # Draw the room circle in brown
    circle_radius = int((SCREEN_WIDTH//2)*.65)
    pygame.draw.circle(screen, BROWN, (SCREEN_WIDTH//2, SCREEN_HEIGHT//2), circle_radius, 0)

    # Next draw all exits from the room
    if exits[LEFT] > 0:
        # Draw left exit
        left = 0
        top = SCREEN_HEIGHT//2-40
        pygame.draw.rect(screen, BROWN, ((left,top), (SCREEN_WIDTH//4,80)), 0)
    if exits[RIGHT] > 0:
        # Draw right exit
        left = SCREEN_WIDTH-(SCREEN_WIDTH//4)
        top = SCREEN_HEIGHT//2-40
        pygame.draw.rect(screen, BROWN, ((left,top), (SCREEN_WIDTH//4,80)), 0)
    if exits[UP] > 0:
        # Draw top exit
        left = SCREEN_WIDTH//2-40
        top = 0
        pygame.draw.rect(screen, BROWN, ((left,top), (80,SCREEN_HEIGHT//4)), 0)
    if exits[DOWN] > 0 :
        # Draw bottom exit
        left = SCREEN_WIDTH//2-40
        top = SCREEN_HEIGHT-(SCREEN_WIDTH//4)
        pygame.draw.rect(screen, BROWN, ((left,top), (80,SCREEN_HEIGHT//4)), 0)
        
    # Find out if bats, pits or a Wumpus is near
    bats_near = check_neighbor_rooms(player_pos, bats_list)
    pit_near = check_neighbor_rooms(player_pos, pits_list)
    wumpus_near = check_neighbor_rooms(player_pos, [wumpus_pos, [-1,-1]])
    
    # Draw a blood circle if the Wumpus is nearby
    #if wumpus_near == True:
        #circle_radius = int((SCREEN_WIDTH//2)*.5)
        #pygame.draw.circle(screen, RED, (SCREEN_WIDTH//2, SCREEN_HEIGHT//2), circle_radius, 0)

    # Draw the pit in black if it is present
    if player_pos in pits_list:
        circle_radius = int((SCREEN_WIDTH//2)*.5)
        pygame.draw.circle(screen, BLACK, (SCREEN_WIDTH//2, SCREEN_HEIGHT//2), circle_radius, 0)
     
    # Draw the player
    screen.blit(player_img,(SCREEN_WIDTH//2-player_img.get_width()//2,SCREEN_HEIGHT//2-player_img.get_height()//2))

    # Draw the bat image
    if player_pos in bats_list:
        screen.blit(bat_img,(SCREEN_WIDTH//2-bat_img.get_width()//2,SCREEN_HEIGHT//2-bat_img.get_height()//2))

    # Draw the Wumpus
    if player_pos == wumpus_pos:
        screen.blit(wumpus_img,(SCREEN_WIDTH//2-wumpus_img.get_width()//2,SCREEN_HEIGHT//2-wumpus_img.get_height()//2))

    # Draw text
    y_text_pos = 0
    pos_text = font.render("POS:"+str(player_pos), 1, (0, 255, 64))
    screen.blit(pos_text,(0, 0))
    arrow_text = font.render("Battery: "+str(num_arrows), 1, (0, 255, 64))
    y_text_pos = y_text_pos+pos_text.get_height()+10
    screen.blit(arrow_text,(0, y_text_pos))
    if bats_near == True:
        bat_text = font.render("You feel a strange wind", 1, (0, 255, 64))
        y_text_pos = y_text_pos+bat_text.get_height()+10
        screen.blit(bat_text,(0, y_text_pos))
    if pit_near == True:
        pit_text = font.render("You feel the ground shaking", 1, (0, 255, 64))
        y_text_pos = y_text_pos+pit_text.get_height()+10
        screen.blit(pit_text,(0, y_text_pos))
    if player_pos in bats_list:
        pygame.display.flip()
        time.sleep(2.0)
        
def populate_cave():
    global player_pos, wumpus_pos

    # Place the player
    player_pos = random.randint(1, 20)

    # Place the wumpus
    place_wumpus()
    
    # Place the bats
    for bat in range(0,NUM_BATS):
        place_bat()

    # Place the pits
    for pit in range (0,NUM_PITS):
        place_pit()

    # Place the arrows
    for flashlight in range (0,NUM_ARROWS):
        place_arrow()

    print ("Player at: "+str(player_pos))


def place_wumpus():
    global player_pos, wumpus_pos
    wumpus_pos = player_pos
    while (wumpus_pos == player_pos):
        wumpus_pos = random.randint(0,20)

def place_bat():
    bat_pos = player_pos
    while bat_pos == player_pos or (bat_pos in bats_list) or (bat_pos == wumpus_pos) or (bat_pos in pits_list):
        bat_pos = random.randint(1,20)
    bats_list.append(bat_pos)

def place_pit():
    pit_pos = player_pos
    while (pit_pos == player_pos) or (pit_pos in bats_list) or (pit_pos == wumpus_pos) or (pit_pos in pits_list):
        pit_pos = random.randint(1,20)
    pits_list.append(pit_pos)

def place_arrow():
    arrow_pos = player_pos
    while (arrow_pos == player_pos) or (arrow_pos in bats_list) or (arrow_pos == wumpus_pos) or (arrow_pos in pits_list):
        arrow_pos = random.randint(1,20)
    arrows_list.append(arrow_pos)
    
def check_room(pos):
    global player_pos, screen, num_arrows
    
    # Is there a Wumpus in the room?
    if player_pos == wumpus_pos:
        breathing_sound.stop()
        jumpscare_sound.play()
        game_over("You died!!!")
        time.sleep(1.0)

    # Is there a pit?
    if player_pos in pits_list:
        game_over("You fell into a bottomless pit!!")

    # Is there bats in the room?  If so move the player and the bats
    if player_pos in bats_list:
        blinded_sound.play()
        print("You have been blinded!")
        screen.fill(BLACK)
        bat_text = font.render("You have been blinded", 1, (0, 255, 64))
        textrect = bat_text.get_rect()
        textrect.centerx = screen.get_rect().centerx
        textrect.centery = screen.get_rect().centery
        screen.blit(bat_text,textrect)
        pygame.display.flip()
        time.sleep(0.5)
        
        # Move the bats
        new_pos = player_pos
        while (new_pos == player_pos) or (new_pos in bats_list) or (new_pos == wumpus_pos) or (new_pos in pits_list):
            new_pos = random.randint(1,20)
        bats_list.remove(player_pos)   
        bats_list.append(new_pos)
        print ("bat at: "+str(new_pos))
                
        # Now move the player
        new_pos = player_pos
        while (new_pos == player_pos) or (new_pos in bats_list) or (new_pos == wumpus_pos) or (new_pos in pits_list):
            new_pos = random.randint(1,20)
        player_pos = new_pos
        print ("player at:"+str(player_pos))

    # Is there an flashlight in the room?
    if player_pos in arrows_list:
        screen.fill(BLACK)
        text = font.render("You have found a battery", 1, (0,20,255))
        textrect = text.get_rect()
        textrect.centerx = screen.get_rect().centerx
        textrect.centery = screen.get_rect().centery
        screen.blit(text,textrect)
        pygame.display.flip()
        time.sleep(1.0)
        num_arrows +=1
        arrows_list.remove(player_pos)
            
def reset_game():
    global num_arrows
    populate_cave()
    num_arrows = 2

def game_over(message):
    global screen
    time.sleep(1.0)
    screen.fill(RED)
    text=font.render(message, 1, (WHITE))
    textrect = text.get_rect()
    textrect.centerx = screen.get_rect().centerx
    textrect.centery = screen.get_rect().centery
    screen.blit(text,textrect)
    pygame.display.flip()
    time.sleep(2.5)
    print(message)
    pygame.quit()
    sys.exit()

def move_wumpus():
    global wumpus_pos, player_pos
    if mobile_wumpus == True or random.randint(1,10) > wumpus_move_chance:
        return
    exits = cave[wumpus_pos]
    for new_room in exits:
        if new_room == 0:
            continue
        elif new_room == player_pos:
            continue
        elif new_room in bats_list:
            continue
        elif new_room in pits_list:
            continue
        else:
            wumpus_pos = new_room
            break
    dx = player_pos[0] - wumpus_pos[0]
    dy = player_pos[1] - wumpus_pos[1]

    # Move towards the player
    if abs(dx) > abs(dy):
        if dx > 0:
            wumpus_pos[0] += 1
        else:
            wumpus_pos[0] -= 1
    else:
        if dy > 0:
            wumpus_pos[1] += 1
        else:
            wumpus_pos[1] -= 1
    wumpus_move_sound.play()
          
def shoot_arrow(direction):
    global num_arrows, player_pos
    global hit
    shoot_sound.play()
    hit = False
    if num_arrows == 0:
        return False
    num_arrows -= 1
    if wumpus_pos == cave[player_pos][direction]:
        hit = True
    if hit:
        breathing_sound.stop()
        x = (SCREEN_WIDTH - dead_wumpus_image.get_width()) // 2
        y = (SCREEN_HEIGHT - dead_wumpus_image.get_height()) // 2
        display_dead_wumpus(x, y)
        pygame.display.flip()
        wumpus_death_sound.play() 
        time.sleep(2.5)
        game_over("You Win!") 
        pygame.quit()
        sys.exit()
    else:    
        place_wumpus()
    if num_arrows == 0:
        game_over("You are out of battery!")
        pygame.quit()
        sys.exit()

def display_dead_wumpus(x, y):
    screen.blit(dead_wumpus_image, (x, y)) 

def check_wumpus_distance(player_pos, wumpus_pos):
    distance = abs(player_pos - wumpus_pos)
    if distance < 2:
        breathing_sound.play(-1)
    else:
        breathing_sound.stop()

def check_pygame_events():
    global player_pos
    event = pygame.event.poll()
    if event.type == pygame.QUIT:
        pygame.quit()
        sys.exit()
    elif event.type == pygame.KEYDOWN:
        if event.key == pygame.K_ESCAPE:
            pygame.quit()
            sys.exit()
        elif event.key ==pygame.K_LEFT:
            if pygame.key.get_mods() & pygame.KMOD_SHIFT:
                shoot_arrow(LEFT)
            elif cave[player_pos][LEFT] > 0: 
                player_pos=cave[player_pos][LEFT]
                move_wumpus()
                footstep_sound.play()
        elif event.key == pygame.K_RIGHT:
            if pygame.key.get_mods() & pygame.KMOD_SHIFT:
                shoot_arrow(RIGHT)
            elif cave[player_pos][RIGHT] > 0:
                player_pos = cave[player_pos][RIGHT]
                move_wumpus()
                footstep_sound.play()
        elif event.key == pygame.K_UP:
            if pygame.key.get_mods() & pygame.KMOD_SHIFT:
                shoot_arrow(UP)
            elif cave[player_pos][UP] > 0:
                player_pos = cave[player_pos][UP]
                move_wumpus()
                footstep_sound.play()
        elif event.key ==pygame.K_DOWN:
            if pygame.key.get_mods() & pygame.KMOD_SHIFT:
                shoot_arrow(DOWN)
            elif cave[player_pos][DOWN] > 0:
                player_pos = cave[player_pos][DOWN]
                move_wumpus()
                footstep_sound.play()

def print_instructions():
    print(
    '''
                                        Game Instructions

    Mechanics:
You have been put into a cave where your objective is to kill the Wumpus using your flashlight.
There are multiple things wandering around the cave. This include:

Wumpus - A monster hungry to eat anything it sees. 
         Avoid running into it by listening for its noises and try using your flashlight to kill it.
         **NOTE** Sometimes the Wumpus will choose to be silent and not making any noises when you are near.
                  To be cautious, trying using your flashlight if you are unsure.
Blinding fogs - If you walk into it, it will blind you and put you into a random room in the cave.
                Try to look for clues to avoid them.
Bottomless pits - If you fell into these pits, it is game over. 
                  Keep an eye out for clues to avoid these.
Batteries - These are used to power your flashlight. 
            Try collecting these to use your flashlight against the Wumpus.
            If you run out of batteries, you will lose.

There is also a timer that you also need to keep an eye out for. 
If the timer goes down to zero before you kill the Wumpus, you will lose.

    Controls:
Use the arrow keys to move between caves.  
Press the <SHIFT> key and an arrow key to use your flashlight at the Wumpus.
    '''
    )

#===============================================================================
#                       Globals and Constants area                             =
#===============================================================================

# Set up the display
SCREEN_WIDTH = 870
SCREEN_HEIGHT= 760
screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT))
clock = pygame.time.Clock()
pygame.display.set_caption("Loading...")

# Images
bat_img = pygame.image.load('images/bat.png')
player_img = pygame.image.load('images/player.png')
wumpus_img = pygame.image.load('images/wumpus.png')
arrow_img = pygame.image.load('images/flashlight.png')
dead_wumpus_image = pygame.image.load('dead_wumpus.png')

# Increase the number of bats and pits to make it harder
# Increase the number of arrows to make it easier
NUM_BATS = 2
NUM_PITS = 0
NUM_ARROWS = 5

# Mechanic settings
num_arrows = 3
mobile_wumpus = True
wumpus_move_chance = 1

# Sound initialization
pygame.mixer.init()
footstep_sound = pygame.mixer.Sound('footsteps.mp3')
jumpscare_sound = pygame.mixer.Sound('jumpscare.mp3')
ambient_sound = pygame.mixer.Sound('ambient.mp3')
breathing_sound = pygame.mixer.Sound('breathing.mp3')
shoot_sound = pygame.mixer.Sound('shoot.mp3')
wumpus_death_sound = pygame.mixer.Sound("wumpus_death.mp3")
blinded_sound = pygame.mixer.Sound('blinded.mp3')
wumpus_move_sound = pygame.mixer.Sound('wumpus_move.mp3')

# Other sound functions
ambient_sound.play(-1)
wumpus_death_sound.set_volume(0.8)
wumpus_move_sound.set_volume(1.5) 
jumpscare_sound.set_volume(1.0)

# Constants for directions
UP = 0
DOWN = 1
LEFT = 2
RIGHT = 3

# Colour definitions
BROWN = 82,64,51
BLACK = 0,0,0
RED = 138,7,7
WHITE = 255,255,255
GREEN = 0,255,0

# Cave mapping
cave = {1: [0,8,2,5], 2: [0,10,3,1], 3: [0,12,4,2], 4: [0,14,5,3], 
    5:[0,6,1,4], 6: [5,0,7,15], 7: [0,17,8,6], 8: [1,0,9,7],
    9: [0,18,10,8], 10: [2,0,11,9], 11: [0,19,12,10], 12: [3,0,13,11], 
    13: [0,20,14,12], 14: [4,0,15,13], 15: [0,16,6,14], 16: [15,0,17,20],
    17: [7,0,18,16], 18: [9,0,19,17], 19: [11,0,20,18], 20: [13,0,16,19] }

bats_list = []
pits_list = []
arrows_list = []

#===============================================================================
#                       Initilizations area                                   =
#===============================================================================

# Startup texts
print_instructions()
input("Press <ENTER> to begin.")
pygame.init()
screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT), pygame.DOUBLEBUF | pygame.HWSURFACE )
pygame.display.set_caption("Hunt the Wumpus")

# Load our three images
bat_img = pygame.image.load('images/bat.png')
player_img = pygame.image.load('images/player.png')
wumpus_img = pygame.image.load('images/wumpus.png')
arrow_img = pygame.image.load('images/flashlight.png')

# Timer variables
countdown_time = 120
start_ticks = pygame.time.get_ticks()

# Define font
font = pygame.font.Font(None, 35)

# Get iniital game settings
reset_game()

#===============================================================================
#                       Main Game Loop                                         =
#===============================================================================

while True:
    check_pygame_events()     
    draw_room(player_pos, screen)
    pygame.display.flip()   
    check_room(player_pos)
    check_wumpus_distance(player_pos, wumpus_pos)
    running = True

   # Check if time is up
    seconds = (pygame.time.get_ticks() - start_ticks) / 1000
    remaining_time = countdown_time - seconds
    if remaining_time <= 0:
        print("You ran out of time!")
        running = False

    # Render the timer
    timer_text = f"Time Left: {int(remaining_time)}"
    font = pygame.font.Font(None, 35)
    text = font.render(timer_text, True, (0, 255, 61))
    x_position = SCREEN_WIDTH - text.get_width() - 10
    y_position = 10
    screen.blit(text, (x_position, y_position))

    # Update the display
    pygame.display.flip()
    pygame.time.Clock().tick(120)
